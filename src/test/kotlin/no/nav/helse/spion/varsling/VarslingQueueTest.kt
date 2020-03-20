package no.nav.helse.spion.varsling

import io.mockk.mockk
import io.mockk.verify
import no.nav.helse.spion.domene.varsling.repository.MockVarslingRepository
import org.junit.jupiter.api.Test

import org.junit.jupiter.api.Assertions.*

internal class VarslingQueueTest {

    @Test
    fun check() {
        val senderMock = mockk<VarslingSender>(relaxed = true)
        val queue = VarslingQueue(MockVarslingRepository())
        queue.check(senderMock)
        verify(exactly = 3) { senderMock.send(any()) }
    }
}