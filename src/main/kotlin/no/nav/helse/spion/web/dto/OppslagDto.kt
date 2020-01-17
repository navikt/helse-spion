package no.nav.helse.spion.web.dto

data class OppslagDto (
    val identitetsnummer: String,
    val arbeidsgiverOrgnr: String?,
    val arbeidsgiverIdentitetsnummer: String?
)