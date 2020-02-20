package no.nav.helse.spion.vedtaksmelding

import java.util.*

data class FailedVedtaksmelding(
        val melding: String,
        val errorMessage: String?,
        val id: UUID = UUID.randomUUID()
)

interface FailedVedtaksmeldingRepository {
    fun save(message: FailedVedtaksmelding)
    fun getNextFailedMessages(numberToGet: Int): Collection<FailedVedtaksmelding>
    fun delete(id: UUID)
}

