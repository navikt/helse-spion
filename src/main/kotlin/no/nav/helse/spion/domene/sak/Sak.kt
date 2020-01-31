package no.nav.helse.spion.domene.sak

import no.nav.helse.spion.domene.Person

data class Sak(
        val person: Person,
        val oppsummering: Oppsummering,
        val ytelsesperioder: List<Ytelsesperiode>
)