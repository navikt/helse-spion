package no.nav.helse.spion.vedtaksmelding.failed

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import no.nav.helse.spion.vedtaksmelding.MessageWithOffset
import no.nav.helse.spion.vedtaksmelding.RecurringJob
import no.nav.helse.spion.vedtaksmelding.VedtaksmeldingService
import java.time.Duration

class FailedVedtaksmeldingProcessor(
        private val failedVedtaksmeldingRepository: FailedVedtaksmeldingRepository,
        private val vedtaksmeldingService: VedtaksmeldingService,
        coroutineScope: CoroutineScope = CoroutineScope(Dispatchers.IO),
        waitTimeWhenEmpty: Duration = Duration.ofHours(3)
) : RecurringJob(coroutineScope, waitTimeWhenEmpty) {

    private val numberOfMessagesToRetryPerRun = 500

    override fun doJob() {
        failedVedtaksmeldingRepository
                .getFailedMessages(numberOfMessagesToRetryPerRun)
                .onEach(this::tryProcessOneMessage)
    }

    private fun tryProcessOneMessage(failed: FailedVedtaksmelding) {
        try {
            vedtaksmeldingService.processAndSaveMessage(MessageWithOffset(failed.kafkaOffset, failed.messageData))
            failedVedtaksmeldingRepository.delete(failed.id)
        } catch (t: Throwable) {
            logger.error("Reprossesering av ${failed.id} feilet", t)
        }
    }
}