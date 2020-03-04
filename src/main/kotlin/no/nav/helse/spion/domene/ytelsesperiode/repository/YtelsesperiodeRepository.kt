package no.nav.helse.spion.domene.ytelsesperiode.repository

import no.nav.helse.spion.domene.Periode
import no.nav.helse.spion.domene.ytelsesperiode.Ytelsesperiode

interface YtelsesperiodeRepository {
    fun getYtelserForPerson(identitetsnummer: String, virksomhetsnummer: String, periode: Periode? = null) : List<Ytelsesperiode>
    fun upsert(yp: Ytelsesperiode)
}