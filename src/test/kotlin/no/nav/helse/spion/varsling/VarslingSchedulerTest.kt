package no.nav.helse.spion.varsling

import io.mockk.mockk
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import no.nav.helse.spion.domene.varsling.repository.MockVarslingRepository
import org.assertj.core.api.Assertions
import org.junit.jupiter.api.Test

internal class VarslingSchedulerTest {

    private val sender = mockk<VarslingSender>(relaxed = true)

    @Test
    fun `Skal forhindre paralelle kjøringer`() {
        val queue = VarslingQueue(MockVarslingRepository())
        val scheduler = VarslingScheduler( 5, queue, sender)
        var first = 1000
        GlobalScope.launch {
            delay(100)
            first = scheduler.start()
        }
        val second = scheduler.start()
        Assertions.assertThat(first).isEqualTo(-1)
        Assertions.assertThat(second).isEqualTo(0)
    }

}