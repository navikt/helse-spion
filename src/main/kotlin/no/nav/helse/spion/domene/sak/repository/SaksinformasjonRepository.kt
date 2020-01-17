package no.nav.helse.spion.domene.sak.repository

import no.nav.helse.spion.domene.sak.Saksinformasjon

interface SaksinformasjonRepository {
    fun hentSaksinformasjonForPerson(token: String, pnr: String) : Saksinformasjon
}