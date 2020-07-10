package no.nav.helse.spion.vedtaksmelding

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import no.nav.helse.spion.vedtaksmelding.failed.FailedVedtaksmelding
import no.nav.helse.spion.vedtaksmelding.failed.FailedVedtaksmeldingRepository
import no.nav.helse.utils.RecurringJob
import java.time.Duration
import java.util.*

class VedtaksmeldingProcessor(
        private val kafkaVedtaksProvider: VedtaksmeldingProvider,
        private val service: VedtaksmeldingService,
        private val failedVedtaksmeldingRepository: FailedVedtaksmeldingRepository,
        coroutineScope: CoroutineScope = CoroutineScope(Dispatchers.IO),
        waitTimeWhenEmptyQueue: Duration = Duration.ofSeconds(30)
) : RecurringJob(coroutineScope, waitTimeWhenEmptyQueue) {

    override fun doJob() {
        do {
            val wasEmpty = kafkaVedtaksProvider
                    .getMessagesToProcess()
                    .onEach { tryProcessOneMessage(it) }
                    .isEmpty()

            if (!wasEmpty) {
                kafkaVedtaksProvider.confirmProcessingDone()
            }
        } while (!wasEmpty)
    }

    private fun tryProcessOneMessage(melding: SpleisMelding) {
        try {
            service.processAndSaveMessage(melding)
        } catch (t: Throwable) {
            val errorId = UUID.randomUUID()
            logger.error("Feilet vedtaksmelding, Database ID: $errorId", t)
            failedVedtaksmeldingRepository.save(FailedVedtaksmelding(melding, t.message, errorId))
        }
    }
}