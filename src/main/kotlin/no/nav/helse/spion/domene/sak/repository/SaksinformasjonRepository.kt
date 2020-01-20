package no.nav.helse.spion.domene.sak.repository

import no.nav.helse.spion.domene.Arbeidsgiver
import no.nav.helse.spion.domene.sak.Sak

interface SaksinformasjonRepository {
    fun hentSakerForPerson(identitetsnummer: String, arbeidsgiverOrgnummer: String, arbeidsgiverIdentitetsnummer: String) : List<Sak>
    fun hentArbeidsgivere(identitetsnummer: String) : List<Arbeidsgiver>

}