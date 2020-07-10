package no.nav.helse.spion.vedtaksmelding

import com.fasterxml.jackson.core.JsonParseException
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import no.nav.helse.spion.domene.ytelsesperiode.repository.YtelsesperiodeRepository
import no.nav.helse.spion.integrasjon.pdl.PdlClient
import no.nav.helse.spion.integrasjon.pdl.PdlHentPerson
import no.nav.helse.spion.integrasjon.pdl.PdlPerson
import no.nav.helse.spion.integrasjon.pdl.PdlPersonNavn
import org.assertj.core.api.Assertions.assertThatExceptionOfType
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.io.IOException

internal class VedtaksmeldingServiceTest {
    val ypDaoMock = mockk<YtelsesperiodeRepository>(relaxed = true)
    val pdlMock = mockk<PdlClient>(relaxed = true)

    val omMock = mockk<ObjectMapper>()

    val meldingsGenerator = SpleisVedtaksmeldingGenerator(maxUniqueArbeidsgivere = 10, maxUniquePersoner = 10)

    val service = VedtaksmeldingService(ypDaoMock, omMock, pdlMock)

    private val mockValidJsonMessage = "mock valid message"
    private val mockInvalidJson = "invalid message"

    @BeforeEach
    internal fun setUp() {
        every { omMock.readValue(mockValidJsonMessage, SpleisVedtakDto::class.java) } answers { mockk<SpleisVedtakDto>() }
        every { omMock.readValue(mockInvalidJson, SpleisVedtakDto::class.java) } throws JsonParseException(null, "error")
        every { pdlMock.person(any()) } returns PdlHentPerson(PdlPerson(listOf(PdlPersonNavn("Ola", "Gunnar", "Normann")), null))
    }

    @Test
    internal fun `successful processing saves To Repository`() {
        service.processAndSaveMessage(SpleisMelding( "fnr", 1, SpleisMeldingstype.Vedtak.name, mockValidJsonMessage))
        verify(exactly = 1) { omMock.readValue(mockValidJsonMessage, SpleisVedtakDto::class.java) }
        verify(exactly = 1) { ypDaoMock.upsert(any()) }
    }

    @Test
    internal fun `If json parsing fails, the error is thrown`() {
        assertThatExceptionOfType(JsonParseException::class.java).isThrownBy {
            service.processAndSaveMessage(SpleisMelding("fnr", 1, SpleisMeldingstype.Vedtak.name, mockInvalidJson))
        }
        verify(exactly = 1) { omMock.readValue(mockInvalidJson, SpleisVedtakDto::class.java) }
        verify(exactly = 0) { ypDaoMock.upsert(any()) }
    }

    @Test
    internal fun `If saving to the DB fails, the error is thrown`() {
        every { ypDaoMock.upsert(any()) } throws IOException()

        assertThatExceptionOfType(IOException::class.java).isThrownBy {
            service.processAndSaveMessage(SpleisMelding("fnr", 1, SpleisMeldingstype.Vedtak.name, mockValidJsonMessage))
        }
        verify(exactly = 1) { omMock.readValue<SpleisVedtakDto>(mockValidJsonMessage) }
        verify(exactly = 1) { ypDaoMock.upsert(any()) }
    }
}
