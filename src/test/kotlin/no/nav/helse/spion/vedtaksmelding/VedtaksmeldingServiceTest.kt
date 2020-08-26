package no.nav.helse.spion.vedtaksmelding

import com.fasterxml.jackson.core.JsonParseException
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.KotlinModule
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import no.nav.helse.spion.domene.ytelsesperiode.repository.YtelsesperiodeRepository
import no.nav.helse.spion.integrasjon.pdl.NameProvider
import org.assertj.core.api.Assertions.assertThatExceptionOfType
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.io.IOException

internal class VedtaksmeldingServiceTest {
    val ypDaoMock = mockk<YtelsesperiodeRepository>(relaxed = true)
    val pdlMock = mockk<NameProvider>(relaxed = true)

    val meldingsGenerator = SpleisVedtaksmeldingGenerator(maxUniqueArbeidsgivere = 10, maxUniquePersoner = 10)

    val service = VedtaksmeldingService(ypDaoMock, ObjectMapper().registerModules(KotlinModule(), JavaTimeModule()), pdlMock)

    @BeforeEach
    internal fun setUp() {
        every { pdlMock.fnrToName(any()) } returns NameProvider.Name("Ola", "Normann")
    }

    @Test
    internal fun `successful processing saves To Repository`() {
        val melding = meldingsGenerator.next()

        service.processAndSaveMessage(melding)
        verify(exactly = 1) { pdlMock.fnrToName(melding.key) }
        verify(exactly = 1) { ypDaoMock.upsert(any()) }
    }

    @Test
    internal fun `If json parsing fails, the error is thrown`() {
        val corruptMelding = SpleisMelding("123", 1L, SpleisMeldingstype.Vedtak.name, "invalid json")

        assertThatExceptionOfType(JsonParseException::class.java).isThrownBy {
            service.processAndSaveMessage(corruptMelding)
        }

        verify(exactly = 0) { ypDaoMock.upsert(any()) }
    }

    @Test
    internal fun `If saving to the DB fails, the error is thrown`() {
        every { ypDaoMock.upsert(any()) } throws IOException()

        assertThatExceptionOfType(IOException::class.java).isThrownBy {
            service.processAndSaveMessage(meldingsGenerator.next())
        }
        verify(exactly = 1) { ypDaoMock.upsert(any()) }
    }
}
