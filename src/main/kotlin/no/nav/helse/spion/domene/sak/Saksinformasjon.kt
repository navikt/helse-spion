package no.nav.helse.spion.domene.sak

data class Saksinformasjon(
    val arbeidsgiver: Arbeidsgiver,
    val person: Person,
    val saksliste: List<Sak>
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