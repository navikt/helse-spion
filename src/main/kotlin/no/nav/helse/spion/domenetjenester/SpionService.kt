package no.nav.helse.spion.domenetjenester

import no.nav.helse.spion.domene.Arbeidsgiver
import no.nav.helse.spion.domene.sak.Sak
import no.nav.helse.spion.domene.sak.repository.SaksinformasjonRepository

class SpionService(private val sakRepo: SaksinformasjonRepository) {
    fun hentSakerForPerson(identitetsnummer: String, arbeidsgiverOrgnummer: String?, arbeidsgiverIdentitetsnummer: String?): List<Sak> {
        return sakRepo.hentSakerForPerson("TODO", "TODO", "TODO")
    }
    fun hentArbeidsgivere(identitet: String) : List<Arbeidsgiver> {
        return sakRepo.hentArbeidsgivere("TODO")
    }
}