package no.nav.helse.spion.vedtaksmelding.failed

import com.fasterxml.jackson.core.JsonParseException
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.test.TestCoroutineDispatcher
import no.nav.helse.spion.vedtaksmelding.VedtaksmeldingService
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.io.IOException

internal class FailedVedtaksmeldingProcessorTest {

    val serviceMock = mockk<VedtaksmeldingService>(relaxed = true)

    val failedMessageDaoMock = mockk<FailedVedtaksmeldingRepository>(relaxed = true)

    val testCoroutineDispatcher = TestCoroutineDispatcher()

    val processor = FailedVedtaksmeldingProcessor(
            failedMessageDaoMock, serviceMock, CoroutineScope(testCoroutineDispatcher)
    )

    private lateinit var msg: List<FailedVedtaksmelding>

    @BeforeEach
    internal fun setUp() {

        msg = listOf(
                FailedVedtaksmelding("data", "error"),
                FailedVedtaksmelding("data", "error")
        )

        every { failedMessageDaoMock.getFailedMessages(any()) } returns msg
    }

    @Test
    internal fun `successful processingMessages saves To Repository and commits To the Queue`() {
        processor.doJob()

        verify(exactly = 2) { serviceMock.processAndSaveMessage("data") }
        verify(exactly = 2) { failedMessageDaoMock.delete(or(msg[0].id, msg[1].id)) }
        verify(exactly = 1) { failedMessageDaoMock.getFailedMessages(any()) }
    }

    @Test
    internal fun `If fetching fails an exception is thrown`() {
        val message = "Error message"

        every { failedMessageDaoMock.getFailedMessages(any()) } throws IOException()

        assertThrows<IOException> { processor.doJob() }

        verify(exactly = 0) { serviceMock.processAndSaveMessage(any()) }
        verify(exactly = 0) { failedMessageDaoMock.delete(any()) }
    }

    @Test
    internal fun `If processing fails, the message is not deleted, next message is tried`() {
        every { serviceMock.processAndSaveMessage(any()) } throws JsonParseException(null, "WRONG")

        processor.doJob()

        verify(exactly = 2) { serviceMock.processAndSaveMessage(any()) }
        verify(exactly = 0) { failedMessageDaoMock.delete(any()) }
    }

    @Test
    internal fun `If processing succeeds but deleting fails, the message is kept and expected to be ignored on next processing`() {
        every { failedMessageDaoMock.delete(any()) } throws IOException()

        processor.doJob()

        verify(exactly = 2) { serviceMock.processAndSaveMessage(any()) }
        verify(exactly = 2) { failedMessageDaoMock.delete(any()) }
    }
}