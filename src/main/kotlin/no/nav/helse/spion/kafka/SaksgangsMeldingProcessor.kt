package no.nav.helse.spion.kafka

import no.nav.helse.spion.db.YtelsesperiodeDao
import org.apache.kafka.clients.consumer.Consumer

class SaksgangsMeldingProcessor(
        val kafkaVedtaksProvider : Consumer<String, VedtaksMelding>,
        val ypDao: YtelsesperiodeDao
) {

}