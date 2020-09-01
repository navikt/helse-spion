package no.nav.helse.spion.vedtaksmelding

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.KotlinModule
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.test.TestCoroutineDispatcher
import no.nav.helse.spion.bakgrunnsjobb.BakgrunnsjobbRepository
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.time.Duration

open class VedtaksmeldingConsumerTests {

    val kafkaMock = mockk<VedtaksmeldingProvider>(relaxed = true)
    val serviceMock = mockk<VedtaksmeldingService>(relaxed = true)
    val repoMock = mockk<BakgrunnsjobbRepository>(relaxed = true)
    val mapper = ObjectMapper()
            .registerModule(KotlinModule())
            .registerModule(JavaTimeModule())

    val meldingsGenerator = VedtaksmeldingGenerator(maxUniqueArbeidsgivere = 10, maxUniquePersoner = 10)

    private val testCoroutineDispatcher = TestCoroutineDispatcher()

    val processor = VedtaksmeldingConsumer(
            kafkaMock, repoMock, mapper, CoroutineScope(testCoroutineDispatcher)
    )

    private lateinit var messageList: List<MessageWithOffset>

    @BeforeEach
    internal fun setUp() {
        messageList = listOf(
                MessageWithOffset(1, mapper.writeValueAsString(meldingsGenerator.next())),
                MessageWithOffset(2, mapper.writeValueAsString(meldingsGenerator.next()))
        )

        every { kafkaMock.getMessagesToProcess() } returnsMany listOf(messageList.map { it.message }, emptyList())
    }

    @Test
    internal fun `successful processingMessages saves To Repository and commits To the Queue`() {
        processor.doJob()

        verify(exactly = 2) { kafkaMock.getMessagesToProcess() }
        verify(exactly = 2) { serviceMock.processAndSaveMessage(any()) }
        verify(exactly = 1) { kafkaMock.confirmProcessingDone() }
        verify(exactly = 1) { repoMock.save(any())}
    }
}

