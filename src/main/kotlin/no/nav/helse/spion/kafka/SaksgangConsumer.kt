package no.nav.helse.spion.kafka

import com.fasterxml.jackson.databind.ObjectMapper
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.apache.kafka.clients.consumer.KafkaConsumer
import org.apache.kafka.common.serialization.StringDeserializer
import org.slf4j.LoggerFactory
import java.time.Duration
import java.util.*

interface KafkaMessageProvider<T> {
    fun getMessages() : List<ConsumerRecord<String, T>>
}

class SaksgangConsumer(props: Map<String, Any>, topicName: String, om: ObjectMapper) : KafkaMessageProvider<VedtaksMelding> {
    private val serdes = VedtaksMeldingSerDes(om)
    private val consumer = KafkaConsumer<String, VedtaksMelding>(props, StringDeserializer(), serdes)

    init {
        consumer.subscribe(Collections.singletonList(topicName));

        Runtime.getRuntime().addShutdownHook(Thread {
            log.debug("Got shutdown message, closing Kafka connection...")
            consumer.close()
            log.debug("Kafka connection closed")
        })
    }

    companion object {
        private val log = LoggerFactory.getLogger(SaksgangConsumer::class.java)
    }

    fun stop() = consumer.close()

    override fun getMessages(): List<ConsumerRecord<String, VedtaksMelding>> {
        println("Asking for messages please")
        return consumer.poll(Duration.ofSeconds(10)).toList()
    }
}

