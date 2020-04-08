package no.nav.helse.spion.varsling

import com.fasterxml.jackson.databind.ObjectMapper
import no.nav.helse.spion.domene.Periode
import no.nav.helse.spion.domene.varsling.PersonVarsling
import no.nav.helse.spion.domene.varsling.Varsling
import no.nav.helse.spion.domene.varsling.repository.VarslingRepository
import no.nav.helse.spion.varsling.mottak.ManglendeInntektsMeldingMelding
import org.slf4j.LoggerFactory
import java.time.LocalDateTime

class VarslingService(
        private val repository: VarslingRepository,
        private val mapper: VarslingMapper,
        private val om: ObjectMapper
) {

    val logger = LoggerFactory.getLogger(VarslingService::class.java)

    fun finnNesteUbehandlet(max: Int, aggregatPeriode: String): List<Varsling> {
        return repository.findByStatus(false, max, aggregatPeriode).map { mapper.mapDomain(it) }
    }

    fun oppdaterStatus(varsling: Varsling, velykket: Boolean) {
        logger.info("Oppdaterer status på ${varsling.uuid} til $velykket")
        repository.updateStatus(varsling.uuid, LocalDateTime.now(), velykket)
    }

    fun lagre(varsling: Varsling) {
        repository.insert(mapper.mapDto(varsling))
    }

    fun slett(uuid: String) {
        repository.remove(uuid)
    }

    fun aggregate(jsonMessageString: String) {
        val kafkaMessage = om.readValue(jsonMessageString, ManglendeInntektsMeldingMelding::class.java)
        logger.info("Fikk en melding fra kafka på virksomhetsnummer ${kafkaMessage.organisasjonsnummer} fra ${kafkaMessage.opprettet}")

        val aggregateStrategy = resolveAggregationStrategy(kafkaMessage)
        val aggregatPeriode = aggregateStrategy.toPeriodeId(kafkaMessage.opprettet.toLocalDate())
        val existingAggregate =
                repository.findByVirksomhetsnummerAndPeriode(kafkaMessage.organisasjonsnummer, aggregatPeriode)

        val person = PersonVarsling(
                kafkaMessage.navn,
                kafkaMessage.fødselsnummer,
                Periode(kafkaMessage.fom, kafkaMessage.tom),
                kafkaMessage.opprettet
        )

        if (existingAggregate == null) {
            logger.info("Det finnes ikke et aggregat på ${kafkaMessage.organisasjonsnummer} for periode $aggregatPeriode, lager en ny")
            val newEntry = Varsling(
                    aggregatPeriode,
                    kafkaMessage.organisasjonsnummer,
                    mutableSetOf(person)
            )
            repository.insert(mapper.mapDto(newEntry))
        } else {
            val  domainVarsling = mapper.mapDomain(existingAggregate)
            logger.info("Fant et aggregat på ${kafkaMessage.organisasjonsnummer} for $aggregatPeriode med ${domainVarsling.liste.size} personer")
            domainVarsling.liste.add(person)
            repository.updateData(domainVarsling.uuid, mapper.mapDto(domainVarsling).data)
        }
    }

    /**
     * Finner strategien som skal brukes for å aggregere varsler for denne meldingen. Kan i fremtiden baseres på org-nr etc
     */
    private fun resolveAggregationStrategy(kafkaMessage: ManglendeInntektsMeldingMelding) = DailyVarslingStrategy()
}