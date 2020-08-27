package no.nav.helse.spion.bakgrunnsjobb

import java.sql.Timestamp
import java.time.LocalDateTime
import java.util.*
import javax.sql.DataSource

interface BakgrunnsjobbRepository {
    fun save(bakgrunnsjobb: Bakgrunnsjobb)
    fun findByKjoeretidBeforeAndStatusIn(timeout: LocalDateTime, tilstander: Set<BakgrunnsjobbStatus>): List<Bakgrunnsjobb>
}

class PostgresBakgrunnsjobbRepository(val dataSource: DataSource) : BakgrunnsjobbRepository {

    private val tableName = "no/nav/helse/spion/bakgrunnsjobb"

    private val insertStatement = """INSERT INTO $tableName
(jobb_id, type, behandlet, opprettet, status, kjoeretid, forsoek, maks_forsoek, data) VALUES
(?::uuid,?,?,?,?,?,?,? ?::json)"""
            .trimIndent()

    private val selectStatement = """
        select * from $tableName where kjoeretid < ? and field = ANY(?)
    """.trimIndent()


    override fun save(bakgrunnsjobb: Bakgrunnsjobb) {
        dataSource.connection.use {
            it.prepareStatement(insertStatement).apply {
                setString(1, bakgrunnsjobb.uuid.toString())
                setString(2, bakgrunnsjobb.type)
                setTimestamp(3, bakgrunnsjobb.behandlet.let(Timestamp::valueOf))
                setTimestamp(4, Timestamp.valueOf(bakgrunnsjobb.opprettet))
                setString(5, bakgrunnsjobb.status.toString())
                setTimestamp(6, Timestamp.valueOf(bakgrunnsjobb.kjoeretid))
                setInt(7, bakgrunnsjobb.forsoek)
                setInt(8, bakgrunnsjobb.maksAntallForsoek)
                setString(9, bakgrunnsjobb.data)
            }
        }
        //signed SIGNED
    }

    override fun findByKjoeretidBeforeAndStatusIn(timeout: LocalDateTime, tilstander: Set<BakgrunnsjobbStatus>): List<Bakgrunnsjobb> {
        dataSource.connection.use {
            val res = it.prepareStatement(selectStatement).apply {
                setTimestamp(1, Timestamp.valueOf(timeout))
                setArray(2, it.createArrayOf("VARCHAR", arrayOf(tilstander.map { status -> status.toString() })))
            }.executeQuery()

            val resultatListe = mutableListOf<Bakgrunnsjobb>()

            //TODO Null safety
            while (res.next()) {
                resultatListe.add(Bakgrunnsjobb(
                        UUID.fromString(res.getString("jobb_id")),
                        res.getString("type"),
                        res.getTimestamp("behandlet").toLocalDateTime(), //Nullable
                        res.getTimestamp("opprettet").toLocalDateTime(),
                        BakgrunnsjobbStatus.valueOf(res.getString("status")),
                        res.getTimestamp("kjoeretid").toLocalDateTime(),
                        res.getInt("forsoek"),
                        res.getInt("maks_forsoek"),
                        res.getString("data")
                ))
            }
            return resultatListe
        }
    }
}
