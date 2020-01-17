package no.nav.helse.spion.domenetjenester

import no.nav.helse.spion.domene.sak.Sak
import no.nav.helse.spion.domene.sak.repository.SaksinformasjonRepository

class SpionService(private val sakRepo: SaksinformasjonRepository) {
    fun hentSakerForPerson(token: String, pnr: String): Sak {
        return sakRepo.hentSakerForPerson("TODO", "TODO")
    }
}