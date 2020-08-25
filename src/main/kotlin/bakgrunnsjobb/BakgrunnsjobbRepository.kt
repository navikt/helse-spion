package bakgrunnsjobb

import java.sql.Date
import java.sql.Timestamp
import java.time.LocalDateTime
import javax.sql.DataSource

interface BakgrunnsjobbRepository {
    fun save(bakgrunnsjobb: Bakgrunnsjobb)
    fun findByKjoeretidBeforeAndStatusIn(timeout: LocalDateTime, tilstander: Set<BakgrunnsjobbStatus>): List<Bakgrunnsjobb>
}

class PostgresBakgrunnsjobbRepository(val dataSource: DataSource) : BakgrunnsjobbRepository {

    private val tableName = "bakgrunnsjobb"

    private val insertStatement = """INSERT INTO $tableName
(jobb_id, type, behandlet, opprettet, status, kjoeretid, forsoek, maks_forsoek, data) VALUES
(?::uuid,?,?,?,?,?,?,? ?::json)"""
            .trimIndent()

    private val selectStatement = """
        select * from $tableName
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
    }

    override fun findByKjoeretidBeforeAndStatusIn(timeout: LocalDateTime, tilstander: Set<BakgrunnsjobbStatus>): List<Bakgrunnsjobb> {
        TODO("Not yet implemented")
    }
}
