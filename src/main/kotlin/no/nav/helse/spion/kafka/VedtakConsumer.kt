package no.nav.helse.spion.kafka

import org.apache.kafka.streams.KafkaStreams
import org.apache.kafka.streams.Topology
import org.slf4j.LoggerFactory
import java.util.*

class VedtakConsumer {

    private val consumer = KafkaStreams(topylogy(), Properties())

    init {
        consumer.addShutdownHook()
        consumer.start()
    }

    companion object {
        private val log = LoggerFactory.getLogger(VedtakConsumer::class.java)
    }

    fun stop() = consumer.close()

    fun state() = consumer.state()

    fun topylogy(): Topology {
        return Topology()
    }


    private fun KafkaStreams.addShutdownHook() {
        setStateListener { newState, oldState ->
            log.info("From state={} to state={}", oldState, newState)
        }

        Runtime.getRuntime().addShutdownHook(Thread {
            close()
        })
    }

}