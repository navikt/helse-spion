package no.nav.helse.spion.varsling

import com.fasterxml.jackson.databind.ObjectMapper
import no.nav.helse.spion.domene.Periode
import no.nav.helse.spion.domene.varsling.PersonVarsling
import no.nav.helse.spion.domene.varsling.Varsling
import no.nav.helse.spion.domene.varsling.repository.VarslingRepository
import no.nav.helse.spion.varsling.mottak.ManglendeInntektsMeldingMelding
import org.slf4j.LoggerFactory
import java.time.LocalDate
import java.time.LocalDateTime

class VarslingService(
        private val repository: VarslingRepository,
        private val mapper: VarslingMapper,
        private val om: ObjectMapper
) {

    val logger = LoggerFactory.getLogger(VarslingService::class.java)

    fun finnNesteUbehandlet(max: Int): List<Varsling> {
        return repository.findByStatus(0, max).map { mapper.mapDomain(it) }
    }

    fun oppdaterStatus(varsling: Varsling, velykket: Boolean) {
        logger.info("Oppdaterer status på ${varsling.uuid} til $velykket")
        if (velykket) {
            repository.updateStatus(varsling.uuid, LocalDateTime.now(), 1)
        } else {
            repository.updateStatus(varsling.uuid, LocalDateTime.now(), 0)
        }
    }

    fun lagre(varsling: Varsling) {
        repository.insert(mapper.mapDto(varsling))
    }

    fun slett(uuid: String) {
        repository.remove(uuid)
    }

    fun aggregate(melding: Pair<LocalDate, String>) {
        val kafkaMessage = om.readValue(melding.second, ManglendeInntektsMeldingMelding::class.java)
        logger.info("Fikk en melding fra kafka på virksomhetsnummer ${kafkaMessage.virksomhetsnummer} fra ${melding.first}")

        val existingAggregate =
                repository.findByVirksomhetsnummerAndDato(kafkaMessage.virksomhetsnummer, melding.first)

        if (existingAggregate == null) {
            logger.info("Det finnes ikke et aggregat på denne virksomheten og datoen, laget er nytt")
            val newEntry = Varsling(
                    melding.first,
                    kafkaMessage.virksomhetsnummer,
                    mutableSetOf(PersonVarsling(
                        kafkaMessage.navn,
                        kafkaMessage.identitetsnummer,
                        Periode(kafkaMessage.fom, kafkaMessage.tom)
                    ))
            )
            repository.insert(mapper.mapDto(newEntry))
        } else {
            val domainVarsling = mapper.mapDomain(existingAggregate)
            domainVarsling.liste.add(PersonVarsling(
                kafkaMessage.navn,
                kafkaMessage.identitetsnummer,
                Periode(kafkaMessage.fom, kafkaMessage.tom)
            ))
            logger.info("Det finnes et aggregat på denne virksomheten og datoen med ${domainVarsling.liste.size} personer, legger til personen i dette")
            repository.update(mapper.mapDto(domainVarsling))
        }
    }
}