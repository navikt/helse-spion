package no.nav.helse.spion.vedtaksmelding

import com.fasterxml.jackson.databind.ObjectMapper
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import no.nav.helse.arbeidsgiver.bakgrunnsjobb.Bakgrunnsjobb
import no.nav.helse.arbeidsgiver.bakgrunnsjobb.BakgrunnsjobbRepository
import no.nav.helse.utils.RecurringJob
import java.time.Duration

class VedtaksmeldingConsumer(
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

    private fun saveMessage(melding: SpleisMelding)
    {
        bakgrunnsjobbRepository.save(Bakgrunnsjobb(
                type = VedtaksmeldingProcessor.JOBB_TYPE,
                data = om.writeValueAsString(melding),
                maksAntallForsoek = 14
        ))
    }
}
