package no.nav.helse.spion.domene.sak.repository

import no.nav.helse.spion.domene.sak.Sak

interface SaksinformasjonRepository {
    fun hentSakerForPerson(token: String, pnr: String) : Sak
}