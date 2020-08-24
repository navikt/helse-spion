package no.nav.helse.slowtests.kafka

import kotlinx.coroutines.runBlocking
import no.nav.helse.spion.vedtaksmelding.SpleisMeldingstype
import no.nav.helse.spion.vedtaksmelding.SpleisVedtaksmeldingGenerator
import no.nav.helse.spion.vedtaksmelding.VedtaksmeldingClient
import no.nav.helse.spion.web.common
import org.apache.kafka.clients.admin.AdminClient
import org.apache.kafka.clients.admin.KafkaAdminClient
import org.apache.kafka.clients.admin.NewTopic
import org.apache.kafka.clients.producer.KafkaProducer
import org.apache.kafka.clients.producer.ProducerRecord
import org.apache.kafka.common.header.internals.RecordHeader
import org.apache.kafka.common.serialization.StringSerializer
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatExceptionOfType
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.koin.core.KoinApplication
import org.koin.core.KoinComponent
import java.util.concurrent.TimeUnit


/**
 * Disse testene krever en kjørende Kafka broker på localhost:9092
 * For å kjøre opp en kan du gjøre
 * cd docker/local
 * docker-compose build
 * docker-compose up
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class VedtaksmeldingClientTest : KoinComponent {
    private lateinit var adminClient: AdminClient
    val topicName = "topic"
    lateinit var koin: KoinApplication
    val generator = SpleisVedtaksmeldingGenerator(maxUniqueArbeidsgivere = 10, maxUniquePersoner = 10)

    val testProps = mutableMapOf<String, Any>(
            "bootstrap.servers" to "localhost:9092",
            "max.poll.interval.ms" to "30000"
    )

    @BeforeAll
    internal fun setUp() {
        koin = KoinApplication.create().modules(common)

        adminClient = KafkaAdminClient.create(testProps)

        adminClient
                .createTopics(mutableListOf(NewTopic(topicName, 1, 1)))
                .all()
                .get(20, TimeUnit.SECONDS)
    }

    @AfterAll
    internal fun tearDown() {
        adminClient.deleteTopics(mutableListOf(topicName))
        adminClient.close()
    }

    @ExperimentalStdlibApi
    @Test
    internal fun testHealthCheck() {
        val client = VedtaksmeldingClient(testProps, topicName)

        runBlocking { client.doHealthCheck() }

        client.stop()

        assertThatExceptionOfType(Exception::class.java).isThrownBy {
            runBlocking { client.getMessagesToProcess() }
        }

        assertThatExceptionOfType(Exception::class.java).isThrownBy {
            runBlocking { client.doHealthCheck() }
        }
    }

    @ExperimentalStdlibApi
    @Test
    fun getMessages() {

        val client = VedtaksmeldingClient(testProps, topicName)
        val noMessagesExpected = client.getMessagesToProcess()

        assertThat(noMessagesExpected).isEmpty()

        val producer = KafkaProducer<String, String>(testProps, StringSerializer(), StringSerializer())
        val generatedMessage = generator.next()
        producer.send(
                ProducerRecord(
                        topicName,
                        0,
                        generatedMessage.key,
                        generatedMessage.messageBody,
                        listOf(RecordHeader("type", SpleisMeldingstype.Vedtak.name.toByteArray()))
                )
        ).get(10, TimeUnit.SECONDS)

        val oneMessageExpected = client.getMessagesToProcess()
        assertThat(oneMessageExpected).hasSize(1)

        client.confirmProcessingDone()

        val zeroMessagesExpected = client.getMessagesToProcess()
        assertThat(zeroMessagesExpected).isEmpty()

        client.stop()
    }
}