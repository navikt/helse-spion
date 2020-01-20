package no.nav.helse.spion.domene

data class Arbeidsgiver(
        val navn: String,
        val orgnr: String,
        val identitetsnummer: String?
)