package no.nav.helse.spion.vedtaksmelding.failed

import java.util.*

class MockFailedVedtaksmeldingsRepository : FailedVedtaksmeldingRepository {
    private val list = mutableListOf<FailedVedtaksmelding>()
    override fun save(message: FailedVedtaksmelding) {
        list.add(message)
    }

    override fun getFailedMessages(numberToGet: Int): Collection<FailedVedtaksmelding> {
        return list
    }

    override fun delete(id: UUID) {
        list.removeIf { it.id == id }
    }
}