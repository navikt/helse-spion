package no.nav.helse.spion.domene.varsling.repository

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import no.nav.helse.spion.domene.varsling.Varsling
import java.sql.Date
import java.sql.ResultSet
import java.sql.Timestamp
import java.time.LocalDateTime
import java.util.*
import javax.sql.DataSource

class PostgresVarslingRepository(private val ds: DataSource, private val mapper: ObjectMapper) : VarslingRepository {

    private val tableName = "varsling"
    private val insertStatement = "INSERT INTO $tableName(data, status, opprettet, virksomhetsNr, uuid, dato) VALUES(?::json, ?, ?, ?, ?::uuid, ?)"
    private val updateStatement = "UPDATE $tableName SET status = ?, behandlet = ? WHERE uuid = ?"
    private val nextStatement = "SELECT * FROM $tableName WHERE status=? ORDER BY opprettet ASC LIMIT ?"
    private val countStatement = "SELECT count(*) FROM $tableName WHERE status = ?"
    private val deleteStatement = "DELETE FROM $tableName WHERE uuid = ?"

    fun mapToDto(varsling: Varsling): VarslingDto {
        return VarslingDto(
                uuid = varsling.uuid,
                data = mapper.writeValueAsString(varsling.liste),
                status = varsling.status,
                opprettet = varsling.opprettet,
                dato = varsling.dato,
                virksomhetsNr = varsling.virksomhetsNr
        )
    }

    fun mapToDto(res: ResultSet): VarslingDto {
        return VarslingDto(
                data = res.getString("data"),
                uuid = res.getString("uuid"),
                status = res.getInt("status"),
                opprettet = res.getTimestamp("opprettet").toLocalDateTime(),
                behandlet = res.getTimestamp("behandlet")?.toLocalDateTime(),
                dato = res.getDate("dato").toLocalDate(),
                virksomhetsNr = res.getString("virksomhetsNr")
        )
    }

    fun mapToDomain(dto: VarslingDto): Varsling {
        return Varsling(
                dato = dto.dato,
                virksomhetsNr = dto.virksomhetsNr,
                uuid = dto.uuid,
                opprettet = dto.opprettet,
                status = dto.status,
                liste = mapper.readValue(dto.data)
        )
    }

    override fun finnNesteUbehandlet(): Varsling {
        return ds.connection.use {
            val resultList = ArrayList<Varsling>()
            val res = it.prepareStatement(nextStatement).apply {
                setInt(1, 0)
                setInt(2, 1)
            }.executeQuery()
            while (res.next()) {
                resultList.add(mapToDomain(mapToDto(res)))
            }
            resultList[0]
        }
    }

    override fun finnAntallUbehandlet(): Int {
        return ds.connection.use {
            val res = it.prepareStatement(countStatement).apply {
                setInt(1, 0)
            }.executeQuery()
            res.next()
            res.getInt(1)
        }
    }

    override fun oppdaterStatus(varsling: Varsling, velykket: Boolean) {
        val status = if (velykket) {
            1
        } else {
            0
        }
        ds.connection.use {
            it.prepareStatement(updateStatement).apply {
                setInt(1, status)
                setTimestamp(2, Timestamp.valueOf(LocalDateTime.now()))
                setString(3, varsling.uuid)
            }.executeUpdate()
        }
    }

    override fun lagre(varsling: Varsling) {
        val dto = mapToDto(varsling)
        ds.connection.use {
            it.prepareStatement(insertStatement).apply {
                setString(1, dto.data)
                setInt(2, dto.status)
                setTimestamp(3, Timestamp.valueOf(dto.opprettet))
                setString(4, dto.virksomhetsNr)
                setString(5, dto.uuid)
                setDate(6, Date.valueOf(dto.dato))
            }.executeUpdate()
        }
    }

    override fun slett(uuid: String) {
        ds.connection.use {
            it.prepareStatement(updateStatement).apply {
                setString(1, uuid)
            }.executeUpdate()
        }
    }

}