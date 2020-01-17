package no.nav.helse.spion.domene.sak

data class Sak(
        val oppsummering: Oppsummering,
        val arbeidsgiver: Arbeidsgiver,
        val person: Person,
        val ytelsesperioder: List<Ytelsesperiode>
)

data class Arbeidsgiver(
        val navn: String,
        val orgnr: String,
        val personnummer: String?
)

data class Person(
        val fornavn: String,
        val etternavn: String
)