package no.nav.helse.spion.kafka

import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.test.TestCoroutineDispatcher
import no.nav.helse.spion.domene.ytelsesperiode.repository.YtelsesperiodeRepository
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.io.IOException
import kotlin.test.assertEquals
import kotlin.test.assertFalse

open class VedtaksmeldingProcessorTests {

    val kafkaMock = mockk<KafkaMessageProvider<Vedtaksmelding>>(relaxed = true)
    val ypDaoMock = mockk<YtelsesperiodeRepository>(relaxed = true)

    val meldingsGenerator = VedtaksmeldingGenerator(10, 10)

    private val testCoroutineDispatcher = TestCoroutineDispatcher()

    val processor = VedtaksmeldingProcessor(
            kafkaMock, ypDaoMock, CoroutineScope(testCoroutineDispatcher)
    )

    @BeforeEach
    internal fun setUp() {
        val messageList = listOf(meldingsGenerator.next())
        every { kafkaMock.getMessagesToProcess() } returnsMany listOf(messageList, emptyList())
    }

    @Test
    internal fun `successful processingMessages saves To Repository and commits To the Queue`() {
        val queueWasEmpty = processor.processOneBatch()
        assertFalse { queueWasEmpty }
        verify(exactly = 1) { kafkaMock.getMessagesToProcess() }
        verify(exactly = 1) { ypDaoMock.save(any()) }
        verify(exactly = 1) { kafkaMock.confirmProcessingDone() }
    }

    @Test
    internal fun `If processing fails, do not confirm processing`() {
        every { ypDaoMock.save(any()) } throws IOException()

        assertThrows<IOException> { processor.processOneBatch() }

        verify(exactly = 1) { ypDaoMock.save(any()) }
        verify(exactly = 0) { kafkaMock.confirmProcessingDone() }
    }

    @Test
    internal fun `StartAsync polls in coroutine untill the queue is empty and then waits`() {
        testCoroutineDispatcher.pauseDispatcher()

        processor.startAsync()
        verify(exactly = 0) { kafkaMock.getMessagesToProcess() }

        testCoroutineDispatcher.runCurrent()

        verify(exactly = 2) { kafkaMock.getMessagesToProcess() }
        verify(exactly = 1) { ypDaoMock.save(any()) }
        verify(exactly = 1) { kafkaMock.confirmProcessingDone() }
    }
}

class VedtaksmeldingMappingTests {
    @Test
    internal fun mappingShouldBeCorrect() {
        val generator = VedtaksmeldingGenerator(10, 10)

        for (i in 0..100) {
            val melding = generator.next()
            val yp = mapVedtaksMeldingTilYtelsesPeriode(melding)

            assertEquals(melding.fom, yp.periode.fom)
            assertEquals(melding.tom, yp.periode.tom)

            assertEquals(melding.fornavn, yp.arbeidsforhold.arbeidstaker.fornavn)
            assertEquals(melding.etternavn, yp.arbeidsforhold.arbeidstaker.etternavn)
            assertEquals(melding.identitetsNummer, yp.arbeidsforhold.arbeidstaker.identitetsnummer)

            assertEquals(melding.virksomhetsnummer, yp.arbeidsforhold.arbeidsgiver.virksomhetsnummer)

            assertEquals(melding.refusjonsbeløp?.toBigDecimal(), yp.refusjonsbeløp)
            assertEquals(melding.dagsats?.toBigDecimal(), yp.dagsats)

            assertEquals(melding.status.correspondingDomainStatus, yp.status)
            assertEquals(melding.maksDato, yp.maxdato)

            assertEquals(melding.sykemeldingsgrad?.toBigDecimal(), yp.grad)
        }
    }
}