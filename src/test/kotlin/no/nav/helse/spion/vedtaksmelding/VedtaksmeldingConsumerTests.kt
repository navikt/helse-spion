package no.nav.helse.spion.vedtaksmelding

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.KotlinModule
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.test.TestCoroutineDispatcher
import no.nav.helse.arbeidsgiver.bakgrunnsjobb.BakgrunnsjobbRepository
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

open class VedtaksmeldingConsumerTests {

    val kafkaMock = mockk<VedtaksmeldingProvider>(relaxed = true)
    val repoMock = mockk<BakgrunnsjobbRepository>(relaxed = true)
    val mapper = ObjectMapper()
        .registerModule(KotlinModule())
        .registerModule(JavaTimeModule())

    val meldingsGenerator = SpleisVedtaksmeldingGenerator(maxUniqueArbeidsgivere = 10, maxUniquePersoner = 10)

    @kotlinx.coroutines.ExperimentalCoroutinesApi
    private val testCoroutineDispatcher = TestCoroutineDispatcher()

    @kotlinx.coroutines.ExperimentalCoroutinesApi
    val processor = VedtaksmeldingConsumer(
        kafkaMock, repoMock, mapper, CoroutineScope(testCoroutineDispatcher)
    )

    private lateinit var messageList: List<SpleisMelding>

    @BeforeEach
    internal fun setUp() {
        messageList = listOf(
            meldingsGenerator.next(),
            meldingsGenerator.next()
        )

        every { kafkaMock.getMessagesToProcess() } returnsMany listOf(messageList, emptyList())
    }

    @Test
    @kotlinx.coroutines.ExperimentalCoroutinesApi
    internal fun `successful processingMessages saves To Repository and commits To the Queue`() {
        processor.doJob()

        verify(exactly = 2) { kafkaMock.getMessagesToProcess() }
        verify(exactly = 2) { repoMock.save(any()) }
        verify(exactly = 1) { kafkaMock.confirmProcessingDone() }
    }
}
