package no.nav.helse.spion.varsling

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import no.nav.helse.spion.vedtaksmelding.RecurringJob
import java.time.Duration
import java.time.LocalDate
import java.time.LocalDateTime

class SendVarslingJob(
        private val service: VarslingService,
        private val sender: VarslingSender,
        coroutineScope: CoroutineScope = CoroutineScope(Dispatchers.IO),
        waitTimeWhenEmptyQueue: Duration = Duration.ofHours(1)
) : RecurringJob(coroutineScope, waitTimeWhenEmptyQueue) {
    val periodeStrategy: VarslingsPeriodeStrategy = DailyVarslingStrategy()

    override fun doJob() {
        val now = LocalDateTime.now()
        if (now.hour < 7 || now.hour > 16) {
            return
        }

        var isEmpty = false
        val prevPeriod = periodeStrategy.previousPeriodeId(LocalDate.now())
        do {
            val varslinger = service.finnNesteUbehandlet(100, prevPeriod)
            isEmpty = varslinger.isEmpty()
            varslinger.forEach {
                sender.send(it)
                service.oppdaterStatus(it, true)
            }
        } while (!isEmpty)
    }

}