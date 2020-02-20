package no.nav.helse.spion.kafka

import no.nav.helse.spion.domene.ytelsesperiode.repository.PostgresRepository
import org.apache.kafka.clients.consumer.Consumer

class SaksgangsMeldingProcessor(
        val kafkaVedtaksProvider : Consumer<String, VedtaksMelding>,
        val ypDao: PostgresRepository
) {

}