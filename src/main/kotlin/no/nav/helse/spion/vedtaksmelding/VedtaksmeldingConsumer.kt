package no.nav.helse.spion.vedtaksmelding

import com.fasterxml.jackson.databind.ObjectMapper
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import no.nav.helse.spion.bakgrunnsjobb.Bakgrunnsjobb
import no.nav.helse.spion.bakgrunnsjobb.BakgrunnsjobbRepository
import java.time.Duration

class VedtaksmeldingConsumer( //TODO Class name
        private val kafkaVedtaksProvider: VedtaksmeldingProvider,
        private val bakgrunnsjobbRepository: BakgrunnsjobbRepository,
        val om: ObjectMapper,
        coroutineScope: CoroutineScope = CoroutineScope(Dispatchers.IO),
        waitTimeWhenEmptyQueue: Duration = Duration.ofSeconds(30)
) : RecurringJob(coroutineScope, waitTimeWhenEmptyQueue) {
    override fun doJob() {
        do {
            val wasEmpty = kafkaVedtaksProvider
                    .getMessagesToProcess()
                    .onEach { saveMessage(it) }
                    .isEmpty()

            if (!wasEmpty) {
                kafkaVedtaksProvider.confirmProcessingDone()
            }
        } while (!wasEmpty)
    }

    private fun saveMessage(melding: String) {
        bakgrunnsjobbRepository.save(Bakgrunnsjobb(
                type = VedtaksmeldingProcessor.JOBB_TYPE,
                data = melding
        ))
    }
}
