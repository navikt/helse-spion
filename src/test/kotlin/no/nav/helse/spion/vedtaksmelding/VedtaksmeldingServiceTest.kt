package no.nav.helse.spion.vedtaksmelding

import com.fasterxml.jackson.core.JsonParseException
import com.fasterxml.jackson.databind.ObjectMapper
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import no.nav.helse.spion.domene.ytelsesperiode.repository.YtelsesperiodeRepository
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatExceptionOfType
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.io.IOException

internal class VedtaksmeldingServiceTest {
    val ypDaoMock = mockk<YtelsesperiodeRepository>(relaxed = true)

    val omMock = mockk<ObjectMapper>()

    val meldingsGenerator = VedtaksmeldingGenerator(maxUniqueArbeidsgivere = 10, maxUniquePersoner = 10)

    val service = VedtaksmeldingService(ypDaoMock, omMock)

    private val mockValidJsonMessage = "mock valid message"
    private val mockInvalidJson = "invalid message"

    @BeforeEach
    internal fun setUp() {
        every { omMock.readValue<Vedtaksmelding>(mockValidJsonMessage, Vedtaksmelding::class.java) } answers { meldingsGenerator.next() }
        every { omMock.readValue<Vedtaksmelding>(mockInvalidJson, Vedtaksmelding::class.java) } throws JsonParseException(null, "error")
    }

    @Test
    internal fun `successful processing deserializes and saves To Repository`() {
        service.processAndSaveMessage(MessageWithOffset(1, mockValidJsonMessage))
        verify(exactly = 1) { omMock.readValue(mockValidJsonMessage, Vedtaksmelding::class.java) }
        verify(exactly = 1) { ypDaoMock.upsert(any()) }
    }

    @Test
    internal fun `If json parsing fails, the error is thrown`() {
        assertThatExceptionOfType(JsonParseException::class.java).isThrownBy {
            service.processAndSaveMessage(MessageWithOffset(1, mockInvalidJson))
        }
        verify(exactly = 1) { omMock.readValue(mockInvalidJson, Vedtaksmelding::class.java) }
        verify(exactly = 0) { ypDaoMock.upsert(any()) }
    }

    @Test
    internal fun `If saving to the DB fails, the error is thrown`() {
        every { ypDaoMock.upsert(any()) } throws IOException()

        assertThatExceptionOfType(IOException::class.java).isThrownBy {
            service.processAndSaveMessage(MessageWithOffset(1, mockValidJsonMessage))
        }
        verify(exactly = 1) { omMock.readValue(mockValidJsonMessage, Vedtaksmelding::class.java) }
        verify(exactly = 1) { ypDaoMock.upsert(any()) }
    }

    @Test
    internal fun mappingShouldBeCorrect() {
        val generator = VedtaksmeldingGenerator(maxUniqueArbeidsgivere = 10, maxUniquePersoner = 10)

        for (i in 0..100) {
            val melding = generator.next()
            val yp = VedtaksmeldingService.mapVedtaksMeldingTilYtelsesPeriode(melding, 1)

            assertThat(yp)
                    .hasFieldOrProperty("periode")
                    .hasFieldOrProperty("arbeidsforhold")
                    .hasFieldOrPropertyWithValue("refusjonsbeløp", melding.refusjonsbeloep?.toBigDecimal())
                    .hasFieldOrPropertyWithValue("dagsats", melding.dagsats?.toBigDecimal())
                    .hasFieldOrPropertyWithValue("status", melding.status.correspondingDomainStatus)
                    .hasFieldOrPropertyWithValue("maxdato", melding.maksDato)
                    .hasFieldOrPropertyWithValue("grad", melding.sykemeldingsgrad?.toBigDecimal())

            assertThat(yp.periode)
                    .hasFieldOrPropertyWithValue("fom", melding.fom)
                    .hasFieldOrPropertyWithValue("tom", melding.tom)

            assertThat(yp.arbeidsforhold.arbeidstaker)
                    .hasFieldOrPropertyWithValue("fornavn", melding.fornavn)
                    .hasFieldOrPropertyWithValue("etternavn", melding.etternavn)
                    .hasFieldOrPropertyWithValue("identitetsnummer", melding.identitetsnummer)

            assertThat(yp.arbeidsforhold.arbeidsgiver)
                    .hasFieldOrPropertyWithValue("arbeidsgiverId", melding.virksomhetsnummer)
        }
    }
}
