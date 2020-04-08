package no.nav.helse.spion.domene.varsling.repository

import java.sql.ResultSet
import java.sql.Timestamp
import java.time.LocalDateTime
import java.util.*
import javax.sql.DataSource

class PostgresVarslingRepository(private val ds: DataSource) : VarslingRepository {

    private val tableName = "varsling"
    private val insertStatement = "INSERT INTO $tableName (data, status, opprettet, virksomhetsNr, uuid, aggregatPeriode) VALUES(?::json, ?, ?, ?, ?::uuid, ?)"

    private val updateDataStatement = "UPDATE $tableName SET data = ?::json WHERE uuid = ?"
    private val updateStatusStatement = "UPDATE $tableName SET status = ?, behandlet = ? WHERE uuid = ?"

    private val deleteStatement = "DELETE FROM $tableName WHERE uuid = ?"
    private val nextStatement = "SELECT * FROM $tableName WHERE status=? AND aggregatPeriode=? LIMIT ?"

    private val getByVirksomhetsnummerAndAggperiode = "SELECT * FROM $tableName WHERE virksomhetsNr=? AND aggregatPeriode=?"

    override fun findByStatus(status: Boolean, max: Int, aggregatPeriode: String): List<VarslingDbEntity> {
        ds.connection.use {
            val resultList = ArrayList<VarslingDbEntity>()
            val res = it.prepareStatement(nextStatement).apply {
                setBoolean(1, status)
                setString(2, aggregatPeriode)
                setInt(3, max)
            }.executeQuery()
            while (res.next()) {
                resultList.add(mapToDto(res))
            }
            return resultList
        }
    }

    override fun findByVirksomhetsnummerAndPeriode(virksomhetsnummer: String, periode: String): VarslingDbEntity? {
        ds.connection.use {
            val resultList = ArrayList<VarslingDbEntity>()
            val res = it.prepareStatement(getByVirksomhetsnummerAndAggperiode).apply {
                setString(1, virksomhetsnummer)
                setString(2, periode)
            }.executeQuery()
            while (res.next()) {
                resultList.add(mapToDto(res))
            }
            return resultList.firstOrNull()
        }
    }

    override fun updateData(uuid: String, data: String) {
        ds.connection.use {
            it.prepareStatement(updateDataStatement).apply {
                setString(1, data)
                setString(2, uuid.toString())
            }.executeUpdate()
        }
    }

    override fun insert(dbEntity: VarslingDbEntity) {
        ds.connection.use {
            it.prepareStatement(insertStatement).apply {
                setString(1, dbEntity.data)
                setBoolean(2, dbEntity.status)
                setTimestamp(3, Timestamp.valueOf(dbEntity.opprettet))
                setString(4, dbEntity.virksomhetsNr)
                setString(5, dbEntity.uuid)
                setString(6, dbEntity.aggregatperiode)
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

    override fun updateStatus(uuid: String, timeOfUpdate: LocalDateTime, status: Boolean) {
        ds.connection.use {
            it.prepareStatement(updateStatusStatement).apply {
                setBoolean(1, status)
                setTimestamp(2, Timestamp.valueOf(timeOfUpdate))
                setString(3, uuid)
            }.executeUpdate()
        }
    }

    private fun mapToDto(res: ResultSet): VarslingDbEntity {
        return VarslingDbEntity(
                data = res.getString("data"),
                uuid = res.getString("uuid"),
                status = res.getBoolean("status"),
                opprettet = res.getTimestamp("opprettet").toLocalDateTime(),
                behandlet = res.getTimestamp("behandlet")?.toLocalDateTime(),
                aggregatperiode = res.getString("aggregatPeriode"),
                virksomhetsNr = res.getString("virksomhetsNr")
        )
    }
}