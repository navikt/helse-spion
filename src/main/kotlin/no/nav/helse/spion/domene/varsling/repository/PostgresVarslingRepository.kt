package no.nav.helse.spion.domene.varsling.repository

import java.sql.Date
import java.sql.Timestamp
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.*
import javax.sql.DataSource

class PostgresVarslingRepository(private val ds: DataSource) : VarslingRepository {

    private val tableName = "varsling"
    private val insertStatement = "INSERT INTO $tableName (data, status, opprettet, virksomhetsNr, uuid, dato, behandlet) VALUES(?::json, ?, ?, ?, ?::uuid, ?, ?)"
    private val updateStatement = "UPDATE $tableName SET data = ?, status = ?, opprettet = ?, virksomhetsNr = ?, dato = ?, behandlet = ? WHERE uuid = ?"
    private val updateStatusStatement = "UPDATE $tableName SET status = ?, behandlet = ? WHERE uuid = ?"
    private val deleteStatement = "DELETE FROM $tableName WHERE uuid = ?"
    private val nextStatement = "SELECT * FROM $tableName WHERE status=? AND dato=? ORDER BY opprettet ASC LIMIT ?"
    private val countStatement = "SELECT count(*) FROM $tableName WHERE status = ? AND dato = ?"
    private val getByVirksomhetsnummerAndDate = "SELECT * FROM $tableName WHERE virksomhetsNr=? AND dato=?"

    override fun findByStatus(dato: LocalDate, status: Int, max: Int): List<VarslingDto> {
        ds.connection.use {
            val resultList = ArrayList<VarslingDto>()
            val res = it.prepareStatement(nextStatement).apply {
                setInt(1, status)
                setDate(2, Date.valueOf(dato))
                setInt(3, max)
            }.executeQuery()
            while (res.next()) {
                resultList.add(mapDto(res))
            }
            return resultList
        }
    }

    override fun countByStatus(dato: LocalDate, status: Int): Int {
        return ds.connection.use {
            val res = it.prepareStatement(countStatement).apply {
                setInt(1, status)
                setDate(2, Date.valueOf(dato))
            }.executeQuery()
            res.next()
            res.getInt(1)
        }
    }

    override fun findByVirksomhetsnummerAndDato(virksomhetsnummer: String, dato: LocalDate): VarslingDto? {
        ds.connection.use {
            val resultList = ArrayList<VarslingDto>()
            val res = it.prepareStatement(getByVirksomhetsnummerAndDate).apply {
                setString(1, virksomhetsnummer)
                setDate(2, Date.valueOf(dato))
            }.executeQuery()
            while (res.next()) {
                resultList.add(mapDto(res))
            }
            return resultList.firstOrNull()
        }
    }

    override fun update(dto: VarslingDto) {
        ds.connection.use {
            it.prepareStatement(updateStatement).apply {
                setString(1, dto.data)
                setInt(2, dto.status)
                setTimestamp(3, Timestamp.valueOf(dto.opprettet))
                setString(4, dto.virksomhetsNr)
                setString(5, dto.uuid)
                setTimestamp(6, if (dto.behandlet == null) { null } else { Timestamp.valueOf(dto.behandlet) })
                setDate(7, Date.valueOf(dto.dato))
            }.executeUpdate()
        }
    }

    override fun save(dto: VarslingDto) {
        ds.connection.use {
            it.prepareStatement(insertStatement).apply {
                setString(1, dto.data)
                setInt(2, dto.status)
                setTimestamp(3, Timestamp.valueOf(dto.opprettet))
                setString(4, dto.virksomhetsNr)
                setString(5, dto.uuid)
                setDate(6, Date.valueOf(dto.dato))
                setTimestamp(7, if (dto.behandlet == null) { null } else { Timestamp.valueOf(dto.behandlet) })
            }.executeUpdate()
        }
    }

    override fun remove(uuid: String) {
        ds.connection.use {
            it.prepareStatement(deleteStatement).apply {
                setString(1, uuid)
            }.executeUpdate()
        }
    }

    override fun updateStatus(uuid: String, dato: LocalDateTime, status: Int) {
        ds.connection.use {
            it.prepareStatement(updateStatusStatement).apply {
                setInt(1, status)
                setTimestamp(2, Timestamp.valueOf(dato))
                setString(3, uuid)
            }.executeUpdate()
        }
    }

}