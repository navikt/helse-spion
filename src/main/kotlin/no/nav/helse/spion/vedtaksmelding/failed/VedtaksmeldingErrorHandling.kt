package no.nav.helse.spion.vedtaksmelding.failed

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import no.nav.helse.spion.vedtaksmelding.SpleisMelding
import java.util.*
import javax.sql.DataSource

data class FailedVedtaksmelding(
        val melding: SpleisMelding,
        val errorMessage: String?,
        val id: UUID = UUID.randomUUID()
)

interface FailedVedtaksmeldingRepository {
    fun save(message: FailedVedtaksmelding)
    fun getFailedMessages(numberToGet: Int): Collection<FailedVedtaksmelding>
    fun delete(id: UUID)
}

class PostgresFailedVedtaksmeldingRepository(val dataSource: DataSource, private val om: ObjectMapper) : FailedVedtaksmeldingRepository {
    private val tableName = "failedvedtaksmelding"

    private val insertStatement = "INSERT INTO $tableName(messageData, errorMessage, id) VALUES(?::json, ?, ?::uuid)"
    private val getStatement = "SELECT * FROM $tableName"
    private val deleteStatement = "DELETE FROM $tableName WHERE id = ?::uuid"

    override fun save(message: FailedVedtaksmelding) {
        dataSource.connection.use {
            it.prepareStatement(insertStatement).apply {
                setString(1, om.writeValueAsString(message.melding))
                setString(2, message.errorMessage)
                setString(3, message.id.toString())
            }.executeUpdate()
        }
    }

    override fun getFailedMessages(numberToGet: Int): Collection<FailedVedtaksmelding> {
        dataSource.connection.use {
            val res = it.prepareStatement("$getStatement LIMIT $numberToGet")
                    .executeQuery()

            val resultatListe = mutableListOf<FailedVedtaksmelding>()

            while (res.next()) {
                resultatListe.add(FailedVedtaksmelding(
                        om.readValue(res.getString("messageData")),
                        res.getString("errorMessage"),
                        UUID.fromString(res.getString("id"))
                ))
            }
            return resultatListe
        }
    }

    override fun delete(id: UUID) {
        dataSource.connection.use {
            it.prepareStatement(deleteStatement).apply {
                setString(1, id.toString())
            }.executeUpdate()
        }
    }
}