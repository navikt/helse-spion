package no.nav.helse.spion.vedtaksmelding

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.KotlinModule
import io.mockk.mockk
import io.mockk.verify
import no.nav.helse.arbeidsgiver.bakgrunnsjobb.Bakgrunnsjobb

import org.junit.jupiter.api.Test

open class VedtaksmeldingProcessorTests {

    val serviceMock = mockk<VedtaksmeldingService>(relaxed = true)
    val generator = SpleisVedtaksmeldingGenerator(maxUniqueArbeidsgivere = 10, maxUniquePersoner = 10)
    val om = ObjectMapper().registerModules(KotlinModule(), JavaTimeModule())
    val processor = VedtaksmeldingProcessor(serviceMock, om)

    @Test
    internal fun `normal melding prosesseres ok`() {
        val jobb = Bakgrunnsjobb(data = om.writeValueAsString(generator.next()), type = "test")
        processor.prosesser(jobb)
        verify(exactly = 1) { serviceMock.processAndSaveMessage(any()) }
    }
}
