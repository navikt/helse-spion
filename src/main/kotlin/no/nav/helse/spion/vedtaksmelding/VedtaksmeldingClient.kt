package no.nav.helse.spion.vedtaksmelding

import no.nav.helse.spion.selfcheck.HealthCheck
import no.nav.helse.spion.selfcheck.HealthCheckType
import org.apache.kafka.clients.consumer.KafkaConsumer
import org.apache.kafka.common.serialization.StringDeserializer
import org.slf4j.LoggerFactory
import java.time.Duration
import java.util.*

interface KafkaMessageProvider {
    fun getMessagesToProcess(): List<String>
    fun confirmProcessingDone()
}

class VedtaksmeldingClient(props: MutableMap<String, Any>, topicName: String, pollWaitWhenEmpty: Int = 30000) : KafkaMessageProvider, HealthCheck {
    private val consumer: KafkaConsumer<String, String>
    override val healthCheckType = HealthCheckType.ALIVENESS

    private val log = LoggerFactory.getLogger(VedtaksmeldingClient::class.java)

    init {
        props.apply {
            put("enable.auto.commit", false)
        }

        consumer = KafkaConsumer<String, String>(props.apply { }, StringDeserializer(), StringDeserializer())
        consumer.subscribe(Collections.singletonList(topicName));

        Runtime.getRuntime().addShutdownHook(Thread {
            log.debug("Got shutdown message, closing Kafka connection...")
            consumer.close()
            log.debug("Kafka connection closed")
        })
    }

    fun stop() = consumer.close()

    override fun getMessagesToProcess(): List<String> {
        return consumer.poll(Duration.ofSeconds(10)).map { it.value() }.toList()
    }

    override fun confirmProcessingDone() {
        consumer.commitSync()
    }

    override suspend fun doHealthCheck() {
        consumer.assignment()
    }
}

