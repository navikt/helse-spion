package no.nav.helse.spion.domenetjenester

import no.nav.helse.spion.domene.Arbeidsgiver
import no.nav.helse.spion.domene.ytelsesperiode.Ytelsesperiode
import no.nav.helse.spion.domene.ytelsesperiode.repository.YtelsesperiodeRepository

class SpionService(private val sakRepo: YtelsesperiodeRepository) {

    fun hentYtelserForPerson(identitetsnummer: String, orgnr: String): List<Ytelsesperiode> {
        return sakRepo.hentYtelserForPerson(identitetsnummer, orgnr)
    }
    fun hentArbeidsgivere(identitet: String) : List<Arbeidsgiver> {
        return sakRepo.hentArbeidsgivere("TODO")
    }
}