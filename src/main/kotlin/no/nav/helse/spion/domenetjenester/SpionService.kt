package no.nav.helse.spion.domenetjenester

import no.nav.helse.spion.domene.sak.Saksinformasjon
import no.nav.helse.spion.domene.sak.repository.SaksinformasjonRepository

class SpionService(private val sakRepo: SaksinformasjonRepository) {
    fun hentSaksinformasjonForPerson(token: String, pnr: String): Saksinformasjon {
        return sakRepo.hentSaksinformasjonForPerson("TODO", "TODO")
    }
}