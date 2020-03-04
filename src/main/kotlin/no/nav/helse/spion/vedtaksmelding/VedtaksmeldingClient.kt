package no.nav.helse.spion.vedtaksmelding

import no.nav.helse.spion.selfcheck.HealthCheck
import no.nav.helse.spion.selfcheck.HealthCheckType
import org.apache.kafka.clients.consumer.KafkaConsumer
import org.apache.kafka.common.serialization.StringDeserializer
import org.slf4j.LoggerFactory
import java.time.Duration
import java.util.*

typealias MessageWithOffset = Pair<Long, String>

interface KafkaMessageProvider {

    fun getMessagesToProcess(): List<MessageWithOffset>
    fun confirmProcessingDone()
}

class VedtaksmeldingClient(props: MutableMap<String, Any>, topicName: String, pollWaitWhenEmpty: Int = 30000) : KafkaMessageProvider, HealthCheck {
    private var lastThrown: Exception? = null
    private val consumer: KafkaConsumer<String, String>
    override val healthCheckType = HealthCheckType.ALIVENESS

    private val log = LoggerFactory.getLogger(VedtaksmeldingClient::class.java)

    init {
        props.apply {
            put("enable.auto.commit", false)
            put("group.id", "helsearbeidsgiver-worker")
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

    override fun getMessagesToProcess(): List<MessageWithOffset> {
        try {
            val result = consumer.poll(Duration.ofSeconds(10)).map { MessageWithOffset(it.offset(), it.value()) }.toList()
            lastThrown = null
            return result
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

