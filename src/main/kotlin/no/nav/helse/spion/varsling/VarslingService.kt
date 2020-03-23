package no.nav.helse.spion.varsling

import com.fasterxml.jackson.databind.ObjectMapper
import no.nav.helse.spion.domene.Periode
import no.nav.helse.spion.domene.varsling.PersonVarsling
import no.nav.helse.spion.domene.varsling.Varsling
import no.nav.helse.spion.domene.varsling.repository.VarslingRepository
import no.nav.helse.spion.varsling.mottak.ManglendeInntektsMeldingMelding
import java.time.LocalDate
import java.time.LocalDateTime

class VarslingService(
        private val repository: VarslingRepository,
        private val mapper: VarslingMapper,
        private val om: ObjectMapper
) {

    fun finnNesteUbehandlet(dato: LocalDate, max: Int): List<Varsling> {
        return repository.findByStatus(dato, 0, max).map { mapper.mapDomain(it) }
    }

    fun oppdaterStatus(varsling: Varsling, velykket: Boolean) {
        repository.updateStatus(varsling.uuid, LocalDateTime.now(), if (velykket) 1 else 0 )
    }

    fun lagre(varsling: Varsling) {
        repository.save(mapper.mapDto(varsling))
    }

    fun slett(uuid: String) {
        repository.remove(uuid)
    }

    fun aggregate(melding: Pair<LocalDate, String>) {
        val kafkaMessage = om.readValue(melding.second, ManglendeInntektsMeldingMelding::class.java)
        val existingAggregate =
                repository.findByVirksomhetsnummerAndDato(kafkaMessage.virksomhetsnummer, melding.first)

        if (existingAggregate == null) {
            val newEntry = Varsling(
                    melding.first,
                    kafkaMessage.virksomhetsnummer,
                    mutableSetOf(PersonVarsling(
                            kafkaMessage.navn,
                            kafkaMessage.identitetsnummer,
                            Periode(kafkaMessage.fom, kafkaMessage.tom)
                    ))
            )
            repository.save(mapper.mapDto(newEntry))
        } else {
            val domainVarsling = mapper.mapDomain(existingAggregate)
            domainVarsling.liste.add(PersonVarsling(
                    kafkaMessage.navn,
                    kafkaMessage.identitetsnummer,
                    Periode(kafkaMessage.fom, kafkaMessage.tom)
            ))
            repository.update(mapper.mapDto(domainVarsling))
        }
    }

}