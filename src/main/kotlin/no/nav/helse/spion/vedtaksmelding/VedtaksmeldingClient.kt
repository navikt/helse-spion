package no.nav.helse.spion.vedtaksmelding

import no.nav.helse.spion.selfcheck.HealthCheck
import no.nav.helse.spion.selfcheck.HealthCheckType
import org.apache.kafka.clients.consumer.KafkaConsumer
import org.apache.kafka.common.header.internals.RecordHeader
import org.apache.kafka.common.serialization.StringDeserializer
import org.slf4j.LoggerFactory
import java.time.Duration
import java.util.*

data class SpleisMelding(val key: String, val offset: Long, val type: String, val messageBody: String)

interface VedtaksmeldingProvider {
    fun getMessagesToProcess(): List<SpleisMelding>
    fun confirmProcessingDone()
}

class VedtaksmeldingClient(props: MutableMap<String, Any>, topicName: String) : VedtaksmeldingProvider, HealthCheck {
    private val missingTypeHeaderDefaultValue = RecordHeader("type", "unknown".toByteArray())
    private var lastThrown: Exception? = null
    private val consumer: KafkaConsumer<String, String>
    override val healthCheckType = HealthCheckType.ALIVENESS

    private val log = LoggerFactory.getLogger(VedtaksmeldingClient::class.java)

    init {
        props.apply {
            put("enable.auto.commit", false)
            put("group.id", "helsearbeidsgiver-mottak")
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

    @ExperimentalStdlibApi
    override fun getMessagesToProcess(): List<SpleisMelding> {
        try {
            val result = consumer.poll(Duration.ofSeconds(10)).map {
                val typeHeader = it.headers().lastHeader("type") ?: missingTypeHeaderDefaultValue
                val messageType =  typeHeader.value().decodeToString()
                SpleisMelding(it.key(), it.offset(), messageType, it.value())
            }.toList()
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

