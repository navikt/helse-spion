package no.nav.helse.spion.domene.ytelsesperiode.repository

import no.nav.helse.spion.domene.Arbeidsgiver
import no.nav.helse.spion.domene.ytelsesperiode.Ytelsesperiode

interface YtelsesperiodeRepository {
    fun hentArbeidsgivere(identitetsnummer: String) : List<Arbeidsgiver>
    fun hentYtelserForPerson(identitetsnummer: String, orgnr: String) : List<Ytelsesperiode>

}