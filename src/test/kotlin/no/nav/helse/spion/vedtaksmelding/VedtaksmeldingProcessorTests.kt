package no.nav.helse.spion.vedtaksmelding

import com.fasterxml.jackson.core.JsonParseException
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.KotlinModule
import com.fasterxml.jackson.module.kotlin.readValue
import io.mockk.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.test.TestCoroutineDispatcher
import no.nav.helse.spion.domene.ytelsesperiode.repository.YtelsesperiodeRepository
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.io.IOException
import kotlin.test.assertEquals
import kotlin.test.assertFalse

open class VedtaksmeldingProcessorTests {

    val kafkaMock = mockk<KafkaMessageProvider>(relaxed = true)
    val ypDaoMock = mockk<YtelsesperiodeRepository>(relaxed = true)
    val failedMessageDaoMock = mockk<FailedVedtaksmeldingRepository>(relaxed = true)
    val mapper = ObjectMapper()
            .registerModule(KotlinModule())
            .registerModule(JavaTimeModule())

    val omMock = mockk<ObjectMapper>()

    val meldingsGenerator = VedtaksmeldingGenerator(10, 10)

    private val testCoroutineDispatcher = TestCoroutineDispatcher()

    val processor = VedtaksmeldingProcessor(
            kafkaMock, ypDaoMock, failedMessageDaoMock, omMock, CoroutineScope(testCoroutineDispatcher)
    )

    private lateinit var messageList: List<String>

    @BeforeEach
    internal fun setUp() {
        messageList = listOf(
                mapper.writeValueAsString(meldingsGenerator.next()),
                mapper.writeValueAsString(meldingsGenerator.next())
        )

        every { omMock.readValue<Vedtaksmelding>(any<String>(), Vedtaksmelding::class.java) } answers { mapper.readValue<Vedtaksmelding>(it.invocation.args[0] as String) }
        every { kafkaMock.getMessagesToProcess() } returnsMany listOf(messageList, emptyList())
    }

    @Test
    internal fun `successful processingMessages saves To Repository and commits To the Queue`() {
        val queueWasEmpty = processor.processOneBatch()
        assertFalse { queueWasEmpty }
        verify(exactly = 1) { kafkaMock.getMessagesToProcess() }
        verify(exactly = 2) { ypDaoMock.save(any()) }
        verify(exactly = 1) { kafkaMock.confirmProcessingDone() }
    }

    @Test
    internal fun `If processing fails, failed message is put into database and processing continues`() {
        val message = "Error message"
        val saveArg = slot<FailedVedtaksmelding>()

        every { omMock.readValue<Vedtaksmelding>(messageList[0], Vedtaksmelding::class.java) } throws JsonParseException(null, message)
        every { failedMessageDaoMock.save(capture(saveArg)) } just Runs

        processor.processOneBatch()

        verify(exactly = 1) { ypDaoMock.save(any()) }
        verify(exactly = 1) { failedMessageDaoMock.save(any()) }
        verify(exactly = 1) { kafkaMock.confirmProcessingDone() }

        assertThat(saveArg.isCaptured).isTrue()
        assertThat(saveArg.captured.errorMessage).isEqualTo(message)
        assertThat(saveArg.captured.id).isNotNull()
        assertThat(saveArg.captured.messageData).isEqualTo(messageList[0])
    }

    @Test
    internal fun `If processing fails and saving the fail fails, throw and do not commit to kafka`() {
        every { omMock.readValue<Vedtaksmelding>(messageList[0], Vedtaksmelding::class.java) } throws JsonParseException(null, "message")
        every { failedMessageDaoMock.save(any()) } throws IOException("DATABSE DOWN")

        assertThrows<IOException> { processor.processOneBatch() }

        verify(exactly = 0) { ypDaoMock.save(any()) }
        verify(exactly = 1) { failedMessageDaoMock.save(any()) }
        verify(exactly = 0) { kafkaMock.confirmProcessingDone() }
    }

    @Test
    internal fun `StartAsync polls in coroutine untill the queue is empty and then waits`() {
        testCoroutineDispatcher.pauseDispatcher()

        processor.startAsync()
        verify(exactly = 0) { kafkaMock.getMessagesToProcess() }

        testCoroutineDispatcher.runCurrent()

        verify(exactly = 2) { kafkaMock.getMessagesToProcess() }
        verify(exactly = 2) { ypDaoMock.save(any()) }
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

            assertEquals(melding.virksomhetsnummer, yp.arbeidsforhold.arbeidsgiver.arbeidsgiverId)

            assertEquals(melding.refusjonsbeløp?.toBigDecimal(), yp.refusjonsbeløp)
            assertEquals(melding.dagsats?.toBigDecimal(), yp.dagsats)

            assertEquals(melding.status.correspondingDomainStatus, yp.status)
            assertEquals(melding.maksDato, yp.maxdato)

            assertEquals(melding.sykemeldingsgrad?.toBigDecimal(), yp.grad)
        }
    }
}