package no.nav.helse.spion.varsling.mottak

import no.nav.helse.spion.selfcheck.HealthCheck
import no.nav.helse.spion.selfcheck.HealthCheckType
import org.apache.kafka.clients.consumer.KafkaConsumer
import org.apache.kafka.common.serialization.StringDeserializer
import org.slf4j.LoggerFactory
import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.util.*

typealias MessageWithDate = Pair<LocalDate, String>

interface ManglendeInntektsmeldingMeldingProvider {
    fun getMessagesToProcess(): List<MessageWithDate>
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
            put("group.id", "helsearbeidsgiver-mottak")
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

    override fun getMessagesToProcess(): List<MessageWithDate> {
        try {
            val result = consumer.poll(Duration.ofSeconds(10)).map {
                MessageWithDate(
                        LocalDate.from(Instant.ofEpochMilli(it.timestamp()).atZone(ZoneId.systemDefault()).toLocalDate()),
                        it.value()
                )
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

