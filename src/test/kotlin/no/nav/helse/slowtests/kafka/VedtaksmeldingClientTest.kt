package no.nav.helse.slowtests.kafka

import no.nav.helse.spion.kafka.*
import no.nav.helse.spion.web.common
import org.apache.kafka.clients.admin.KafkaAdminClient
import org.apache.kafka.clients.admin.NewTopic
import org.apache.kafka.clients.producer.KafkaProducer
import org.apache.kafka.clients.producer.ProducerRecord
import org.apache.kafka.common.serialization.StringSerializer
import org.junit.Assert
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.koin.core.KoinComponent
import org.koin.core.context.loadKoinModules
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import org.koin.core.get
import java.time.LocalDate
import java.util.concurrent.TimeUnit


/**
 * Disse testene krever en kjørende Kafka broker på localhost:9092
 * For å kjøre opp en kan du gjøre
 * cd docker/local
 * docker-compose build
 * docker-compose up
 */
internal class VedtaksmeldingClientTest : KoinComponent {
    val topicName = "topic"

    val testProps = mapOf(
            "bootstrap.servers" to "localhost:9092",
            "group.id" to "juicey"
    )

    @BeforeEach
    internal fun setUp() {
        startKoin {
            loadKoinModules(common)
        }

        val adminClient = KafkaAdminClient.create(testProps)

        adminClient
                .createTopics(mutableListOf(NewTopic(topicName, 1, 1)))
                .all()
                .get(20, TimeUnit.SECONDS)
    }

    @AfterEach
    internal fun tearDown() {
        stopKoin()
        val adminClient = KafkaAdminClient.create(testProps)
        adminClient.deleteTopics(mutableListOf(topicName))
    }

    @Test
    fun getMessages() {

        val client = VedtaksmeldingClient(testProps, topicName, get())
        val noMessagesExpected = client.getMessagesToProcess()

        Assert.assertEquals(0, noMessagesExpected.size)

        val producer = KafkaProducer<String, Vedtaksmelding>(testProps, StringSerializer(), VedtaksMeldingSerDes(get()))

        producer.send(
                ProducerRecord(topicName, Vedtaksmelding(
                        "222323",
                        "323232323",
                        VedtaksmeldingsStatus.BEHANDLES,
                        LocalDate.now(),
                        LocalDate.now(),
                        VedtaksmeldingsYtelse.SP,
                        "Hans",
                        "Ingenmann",
                        100,
                        938293.9,
                        2387.0,
                        maksDato = LocalDate.now().plusDays(10)
                ))
        ).get(10, TimeUnit.SECONDS)

        val oneMessageExpected = client.getMessagesToProcess()

        Assert.assertEquals(1, oneMessageExpected.size)
    }
}