package no.nav.helse.spion.kafka

import com.fasterxml.jackson.databind.ObjectMapper
import no.nav.helse.spion.selfcheck.HealthCheck
import no.nav.helse.spion.selfcheck.HealthCheckType
import org.apache.kafka.clients.consumer.KafkaConsumer
import org.apache.kafka.common.serialization.StringDeserializer
import org.slf4j.LoggerFactory
import java.time.Duration
import java.util.*

interface KafkaMessageProvider<T> {
    fun getMessagesToProcess(): List<T>
    fun confirmProcessingDone()
}

class VedtaksmeldingClient(props: Map<String, Any>, topicName: String, om: ObjectMapper) : KafkaMessageProvider<Vedtaksmelding>, HealthCheck {
    private val serdes = VedtaksMeldingSerDes(om)
    private val consumer = KafkaConsumer<String, Vedtaksmelding>(props, StringDeserializer(), serdes)
    override val healthCheckType = HealthCheckType.ALIVENESS

    private val log = LoggerFactory.getLogger(VedtaksmeldingClient::class.java)

    init {
        consumer.subscribe(Collections.singletonList(topicName));

        Runtime.getRuntime().addShutdownHook(Thread {
            log.debug("Got shutdown message, closing Kafka connection...")
            consumer.close()
            log.debug("Kafka connection closed")
        })
    }

    fun stop() = consumer.close()

    override fun getMessagesToProcess(): List<Vedtaksmelding> {
        return consumer.poll(Duration.ofSeconds(10)).map { it.value() }.toList()
    }

    override fun confirmProcessingDone() {
        consumer.commitSync()
    }

    override suspend fun doHealthCheck() {
        consumer.assignment()
    }
}

