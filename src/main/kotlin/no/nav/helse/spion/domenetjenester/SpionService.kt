package no.nav.helse.spion.domenetjenester

import no.nav.helse.spion.domene.saksinformasjon.Saksinformasjon
import no.nav.helse.spion.domene.saksinformasjon.repository.SaksinformasjonRepository

class SpionService(private val sakRepo: SaksinformasjonRepository) {

    fun hentSaksinformasjon(): Saksinformasjon {
        return sakRepo.hentSaksinformasjon("TODO")
    }
}