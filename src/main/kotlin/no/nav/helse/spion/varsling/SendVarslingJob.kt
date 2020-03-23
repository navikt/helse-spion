package no.nav.helse.spion.varsling

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import no.nav.helse.spion.vedtaksmelding.RecurringJob
import java.time.Duration
import java.time.LocalDate

class SendVarslingJob(
        private val service: VarslingService,
        private val sender: VarslingSender,
        coroutineScope: CoroutineScope = CoroutineScope(Dispatchers.IO),
        waitTimeWhenEmptyQueue: Duration = Duration.ofSeconds(30)
) : RecurringJob(coroutineScope, waitTimeWhenEmptyQueue) {

    override fun doJob() {
        findAndSend(LocalDate.now().minusDays(1), service, sender)
    }

}

fun findAndSend(dato: LocalDate, service: VarslingService, sender: VarslingSender){
    var isEmpty = false
    do {
        val varslinger = service.finnNesteUbehandlet(dato, 10)
        isEmpty = varslinger.isEmpty()
        varslinger.forEach {
            sender.send(it)
            service.oppdaterStatus(it, true)
        }
    } while (isEmpty)
}