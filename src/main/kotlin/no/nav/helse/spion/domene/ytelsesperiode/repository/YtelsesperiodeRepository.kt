package no.nav.helse.spion.domene.ytelsesperiode.repository

import no.nav.helse.spion.domene.ytelsesperiode.Ytelsesperiode

interface YtelsesperiodeRepository {
    fun hentYtelserForPerson(identitetsnummer: String, virksomhetsnummer: String) : List<Ytelsesperiode>
    fun upsert(yp: Ytelsesperiode)
}