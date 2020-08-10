package no.nav.helse.spion.domene.sykepengesoeknad

import no.nav.helse.spion.domene.Periode
import org.slf4j.LoggerFactory
import java.util.*
import javax.sql.DataSource

class PostgresSykepengesoeknadRepository(val ds: DataSource) : SykepengesoeknadRepository {
    private val logger = LoggerFactory.getLogger(PostgresSykepengesoeknadRepository::class.java)
    private val tableName = "soeknad_periode"

    private val getById = """SELECT * FROM $tableName WHERE id = ?::uuid;"""

    private val upsertStatement = """INSERT INTO $tableName (id, fom, tom) VALUES (?::uuid, ?, ?) 
        ON CONFLICT (id) 
        DO UPDATE SET fom = ?, tom = ? WHERE $tableName.id = ?::uuid;""".trimMargin()

    private val deleteStatement = """DELETE  FROM $tableName WHERE id = ?::uuid;"""

    override fun upsert(entity: Sykepengesoeknad): Int {
        ds.connection.use {con ->
            return con.prepareStatement(upsertStatement).apply {
                setString(1, entity.uuid.toString())
                setString(2, entity.periode.fom.toString())
                setString(3, entity.periode.tom.toString())

                setString(4, entity.periode.fom.toString())
                setString(5, entity.periode.tom.toString())
                setString(6, entity.uuid.toString())

            }.executeUpdate()
        }
    }

    fun delete(id: UUID): Int {
        ds.connection.use { con ->
            val deletedCount = con.prepareStatement(deleteStatement).apply {
                setString(1, id.toString())
            }.executeUpdate()
            return deletedCount
        }
    }

    override fun getById(id: UUID): Sykepengesoeknad? {
        ds.connection.use { con ->
            val res = con.prepareStatement(getById).apply {
                setString(1, id.toString())
            }.executeQuery()

            return if (res.next()) {
                val uuid = UUID.fromString(res.getString("id"))
                return Sykepengesoeknad(uuid, Periode(res.getString("fom"), res.getString("tom")))
            } else null
        }
    }
}