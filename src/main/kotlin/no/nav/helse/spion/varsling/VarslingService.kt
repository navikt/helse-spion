package no.nav.helse.spion.varsling

import no.nav.helse.spion.domene.varsling.Varsling
import no.nav.helse.spion.domene.varsling.repository.VarslingRepository
import java.time.LocalDateTime

class VarslingService(
        private val repository: VarslingRepository,
        private val mapper: VarslingMapper
) {

    fun finnNesteUbehandlet(max: Int): List<Varsling> {
        return repository.findByStatus(0, max).map { mapper.mapDomain(it) }
    }

    fun oppdaterStatus(varsling: Varsling, velykket: Boolean) {
        repository.updateStatus(varsling.uuid, LocalDateTime.now(),1)
    }

    fun lagre(varsling: Varsling) {
        repository.insert(mapper.mapDto(varsling))
    }

    fun slett(uuid: String) {
        repository.remove(uuid)
    }

}