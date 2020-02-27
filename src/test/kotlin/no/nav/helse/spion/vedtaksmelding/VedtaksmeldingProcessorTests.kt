package no.nav.helse.spion.vedtaksmelding

import com.fasterxml.jackson.core.JsonParseException
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.KotlinModule
import io.mockk.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.test.TestCoroutineDispatcher
import no.nav.helse.spion.vedtaksmelding.failed.FailedVedtaksmelding
import no.nav.helse.spion.vedtaksmelding.failed.FailedVedtaksmeldingRepository
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.io.IOException
import java.time.Duration

open class VedtaksmeldingProcessorTests {

    val kafkaMock = mockk<KafkaMessageProvider>(relaxed = true)
    val serviceMock = mockk<VedtaksmeldingService>(relaxed = true)
    val failedMessageDaoMock = mockk<FailedVedtaksmeldingRepository>(relaxed = true)
    val mapper = ObjectMapper()
            .registerModule(KotlinModule())
            .registerModule(JavaTimeModule())

    val meldingsGenerator = VedtaksmeldingGenerator(maxUniqueArbeidsgivere = 10, maxUniquePersoner = 10)

    private val testCoroutineDispatcher = TestCoroutineDispatcher()

    val processor = VedtaksmeldingProcessor(
            kafkaMock, serviceMock, failedMessageDaoMock, CoroutineScope(testCoroutineDispatcher), Duration.ofMillis(100)
    )

    private lateinit var messageList: List<String>

    @BeforeEach
    internal fun setUp() {
        messageList = listOf(
                mapper.writeValueAsString(meldingsGenerator.next()),
                mapper.writeValueAsString(meldingsGenerator.next())
        )

        every { kafkaMock.getMessagesToProcess() } returnsMany listOf(messageList, emptyList())
    }

    @Test
    internal fun `successful processingMessages saves To Repository and commits To the Queue`() {
        processor.doJob()

        verify(exactly = 2) { kafkaMock.getMessagesToProcess() }
        verify(exactly = 2) { serviceMock.processAndSaveMessage(any()) }
        verify(exactly = 1) { kafkaMock.confirmProcessingDone() }
    }

    @Test
    internal fun `If processing fails, failed message is put into database and processing continues`() {
        val message = "Error message"
        val saveArg = slot<FailedVedtaksmelding>()

        every { serviceMock.processAndSaveMessage(messageList[0]) } throws JsonParseException(null, message)
        every { failedMessageDaoMock.save(capture(saveArg)) } just Runs

        processor.doJob()

        verify(exactly = 2) { serviceMock.processAndSaveMessage(any()) }
        verify(exactly = 1) { failedMessageDaoMock.save(any()) }
        verify(exactly = 1) { kafkaMock.confirmProcessingDone() }

        assertThat(saveArg.isCaptured).isTrue()
        assertThat(saveArg.captured.errorMessage).isEqualTo(message)
        assertThat(saveArg.captured.id).isNotNull()
        assertThat(saveArg.captured.messageData).isEqualTo(messageList[0])
    }

    @Test
    internal fun `If processing fails and saving the fail fails, throw and do not commit to kafka`() {
        every { serviceMock.processAndSaveMessage(messageList[0]) } throws JsonParseException(null, "WRONG")
        every { failedMessageDaoMock.save(any()) } throws IOException("DATABSE DOWN")

        assertThrows<IOException> { processor.doJob() }

        verify(exactly = 1) { serviceMock.processAndSaveMessage(messageList[0]) }
        verify(exactly = 1) { failedMessageDaoMock.save(any()) }
        verify(exactly = 0) { kafkaMock.confirmProcessingDone() }
    }

}

