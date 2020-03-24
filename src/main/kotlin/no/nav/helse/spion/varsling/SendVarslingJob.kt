package no.nav.helse.spion.varsling

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import no.nav.helse.spion.vedtaksmelding.RecurringJob
import java.time.Duration

class SendVarslingJob(
        private val service: VarslingService,
        private val sender: VarslingSender,
        coroutineScope: CoroutineScope = CoroutineScope(Dispatchers.IO),
        waitTimeWhenEmptyQueue: Duration = Duration.ofSeconds(30)
) : RecurringJob(coroutineScope, waitTimeWhenEmptyQueue) {

    override fun doJob() {
        var isEmpty = false
        do {
            val varslinger = service.finnNesteUbehandlet(10)
            isEmpty = varslinger.isEmpty()
            varslinger.forEach {
                sender.send(it)
                service.oppdaterStatus(it, true)
            }
        } while (!isEmpty)
    }

}