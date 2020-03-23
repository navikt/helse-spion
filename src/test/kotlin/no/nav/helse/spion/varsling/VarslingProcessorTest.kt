package no.nav.helse.spion.varsling

import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import no.nav.helse.spion.domene.varsling.Varsling
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.time.LocalDateTime

internal class VarslingProcessorTest {

    @Test
    fun `Skal sende`() {
        val varsling = Varsling(
                uuid = "123",
                status = 1,
                virksomhetsNr = "123456789",
                opprettet = LocalDateTime.now(),
                dato = LocalDate.now(),
                liste = mutableSetOf()
        )

        val service = mockk<VarslingService>()
        val sender = mockk<VarslingSender>()
        every { service.finnNesteUbehandlet(any(), any()) } returns listOf(varsling, varsling.copy(uuid = "456"))
        every { sender.send(any()) } returns Unit
        every { service.oppdaterStatus(any(), any() ) } returns Unit
        val dato = LocalDate.now().minusDays(1)
        findAndSend(dato, service, sender)
        verify(exactly = 1) { service.finnNesteUbehandlet(any(), 10) }
        verify(exactly = 2) { sender.send(any()) }
        verify(exactly = 2) { service.oppdaterStatus(any(), true) }
    }
}