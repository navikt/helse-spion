package no.nav.helse.spion.vedtaksmelding

import kotlinx.coroutines.CoroutineScope
import no.nav.helse.spion.vedtaksmelding.failed.FailedVedtaksmelding
import no.nav.helse.spion.vedtaksmelding.failed.FailedVedtaksmeldingRepository
import java.time.Duration
import java.util.*

class VedtaksmeldingProcessor(
        val kafkaVedtaksProvider: KafkaMessageProvider,
        val service: VedtaksmeldingService,
        val failedVedtaksmeldingRepository: FailedVedtaksmeldingRepository,
        coroutineScope: CoroutineScope,
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

    private fun tryProcessOneMessage(melding: String) {
        try {
            service.processAndSaveMessage(melding)
        } catch (t: Throwable) {
            val errorId = UUID.randomUUID()
            logger.error("Feilet vedtaksmelding, Database ID: $errorId", t)
            failedVedtaksmeldingRepository.save(FailedVedtaksmelding(melding, t.message, errorId))
        }
    }
}