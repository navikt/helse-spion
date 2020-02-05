package no.nav.helse.spion.domene

data class Arbeidsgiver(
        val navn: String,
        val organisasjonsnummer: String?,
        val virksomhetsnummer: String?,
        val identitetsnummer: String?
)