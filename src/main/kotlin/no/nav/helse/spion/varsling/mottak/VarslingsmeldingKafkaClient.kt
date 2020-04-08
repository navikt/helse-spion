package no.nav.helse.spion.varsling.mottak

import no.nav.helse.spion.selfcheck.HealthCheck
import no.nav.helse.spion.selfcheck.HealthCheckType
import org.apache.kafka.clients.consumer.KafkaConsumer
import org.apache.kafka.common.serialization.StringDeserializer
import org.slf4j.LoggerFactory
import java.time.Duration
import java.util.*

interface ManglendeInntektsmeldingMeldingProvider {
    fun getMessagesToProcess(): List<String>
    fun confirmProcessingDone()
}

class VarslingsmeldingKafkaClient(props: MutableMap<String, Any>, topicName: String) : ManglendeInntektsmeldingMeldingProvider, HealthCheck {
    private var lastThrown: Exception? = null
    private val consumer: KafkaConsumer<String, String>
    override val healthCheckType = HealthCheckType.ALIVENESS

    private val log = LoggerFactory.getLogger(VarslingsmeldingKafkaClient::class.java)

    init {
        props.apply {
            put("enable.auto.commit", false)
            put("group.id", "helsearbeidsgiver-varsling-mottak")
            put("max.poll.interval.ms", Duration.ofMinutes(60).toMillis().toInt())
            put("auto.offset.reset", "earliest")
        }

        consumer = KafkaConsumer<String, String>(props, StringDeserializer(), StringDeserializer())
        consumer.subscribe(Collections.singletonList(topicName));

        Runtime.getRuntime().addShutdownHook(Thread {
            log.debug("Got shutdown message, closing Kafka connection...")
            consumer.close()
            log.debug("Kafka connection closed")
        })
    }

    fun stop() = consumer.close()

    override fun getMessagesToProcess(): List<String> {
        try {
            val kafkaMessages = consumer.poll(Duration.ofSeconds(10))
            val payloads = kafkaMessages.map {it.value() }
            lastThrown = null
            val offsets = kafkaMessages.map { it }
            log.debug("Fikk ${kafkaMessages.count()} meldinger med offsets ${kafkaMessages.map { it.offset() }.joinToString(", ")}")
            return payloads
        } catch (e: Exception) {
            lastThrown = e
            throw e
        }
    }

    override fun confirmProcessingDone() {
        consumer.commitSync()
    }

    override suspend fun doHealthCheck() {
        lastThrown?.let { throw lastThrown as Exception }
    }
}

