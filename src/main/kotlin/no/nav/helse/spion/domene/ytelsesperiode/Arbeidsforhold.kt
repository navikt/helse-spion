package no.nav.helse.spion.domene.ytelsesperiode

import no.nav.helse.spion.domene.Arbeidsgiver
import no.nav.helse.spion.domene.Person

data class Arbeidsforhold(
        val arbeidsforholdId: String,
        val arbeidstaker: Person,
        val arbeidsgiver: Arbeidsgiver
)