package no.nav.helse.spion.vedtaksmelding.failed

import kotlinx.coroutines.CoroutineScope
import no.nav.helse.spion.vedtaksmelding.ScheduledJob
import no.nav.helse.spion.vedtaksmelding.VedtaksmeldingService
import java.time.Duration

class FailedVedtaksmeldingProcessor(
        val failedVedtaksmeldingRepository: FailedVedtaksmeldingRepository,
        val vedtaksmeldingService: VedtaksmeldingService,
        coroutineScope: CoroutineScope,
        waitTimeWhenEmpty: Duration = Duration.ofHours(3)
) : ScheduledJob(coroutineScope, waitTimeWhenEmpty) {

    private val numberOfMessagesToRetryPerRun = 500

    override fun doJob() {
        failedVedtaksmeldingRepository
                .getFailedMessages(numberOfMessagesToRetryPerRun)
                .onEach(this::tryProcessOneMessage)
    }

    private fun tryProcessOneMessage(failed: FailedVedtaksmelding) {
        try {
            vedtaksmeldingService.processAndSaveMessage(failed.messageData)
            failedVedtaksmeldingRepository.delete(failed.id)
        } catch (t: Throwable) {
            logger.error("Reprossesering av ${failed.id} feilet", t)
        }
    }
}