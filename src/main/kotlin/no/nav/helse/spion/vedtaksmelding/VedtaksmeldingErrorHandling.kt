package no.nav.helse.spion.vedtaksmelding

import java.util.*
import javax.sql.DataSource

data class FailedVedtaksmelding(
        val messageData: String,
        val errorMessage: String?,
        val id: UUID = UUID.randomUUID()
)

interface FailedVedtaksmeldingRepository {
    fun save(message: FailedVedtaksmelding)
    fun getNextFailedMessages(numberToGet: Int): Collection<FailedVedtaksmelding>
    fun delete(id: UUID)
}

class PostgresFailedVedtaksmeldingRepository(val dataSource: DataSource) : FailedVedtaksmeldingRepository {
    private val tableName = "failedvedaksmelding"

    private val insertStatement = "INSERT INTO $tableName(messageData, errorMessage, id) VALUES(?, ?, ?)"
    private val getStatement = "SELECT * FROM $tableName"
    private val deleteStatement = "DELETE FROM $tableName WHERE id = ?"

    override fun save(message: FailedVedtaksmelding) {
        dataSource.connection.use {
            it.prepareStatement(insertStatement).apply {
                setString(1, message.messageData)
                setString(2, message.errorMessage)
                setString(3, message.id.toString())
            }.executeUpdate()
        }
    }

    override fun getNextFailedMessages(numberToGet: Int): Collection<FailedVedtaksmelding> {
        dataSource.connection.use {
            val res = it.prepareStatement(getStatement + " LIMIT $numberToGet")
                    .executeQuery()

            val resultatListe = mutableListOf<FailedVedtaksmelding>()

            while (res.next()) {
                resultatListe.add(FailedVedtaksmelding(
                        res.getString("messageData"),
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