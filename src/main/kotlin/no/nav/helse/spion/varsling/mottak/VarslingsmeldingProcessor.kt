package no.nav.helse.spion.varsling.mottak

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import no.nav.helse.spion.varsling.VarslingService
import no.nav.helse.spion.vedtaksmelding.RecurringJob
import java.time.Duration

class VarslingsmeldingProcessor(
        private val kafkaProvider: ManglendeInntektsmeldingMeldingProvider,
        private val service: VarslingService,
        coroutineScope: CoroutineScope = CoroutineScope(Dispatchers.IO),
        waitTimeWhenEmptyQueue: Duration = Duration.ofMinutes(30)
) : RecurringJob(coroutineScope, waitTimeWhenEmptyQueue) {

    override fun doJob() {
        do {
            val wasEmpty = kafkaProvider
                    .getMessagesToProcess()
                    .onEach(service::aggregate)
                    .isEmpty()

            if (!wasEmpty) {
                kafkaProvider.confirmProcessingDone()
            }
        } while (!wasEmpty)
    }
}