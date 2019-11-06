package no.nav.helse.spion.domene.saksinformasjon.repository

import no.nav.helse.spion.domene.saksinformasjon.Saksinformasjon

interface SaksinformasjonRepository {
    fun hentSaksinformasjon(aktørId: String): Saksinformasjon
}