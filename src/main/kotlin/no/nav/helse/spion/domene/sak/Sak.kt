package no.nav.helse.spion.domene.sak

import no.nav.helse.spion.domene.Arbeidsgiver
import no.nav.helse.spion.domene.Person

data class Sak(
        val person: Person,
        val arbeidsgiver: Arbeidsgiver,
        val oppsummering: Oppsummering,
        val ytelsesperioder: List<Ytelsesperiode>
)