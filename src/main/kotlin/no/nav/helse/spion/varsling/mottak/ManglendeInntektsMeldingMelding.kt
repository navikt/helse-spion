package no.nav.helse.spion.varsling.mottak

import java.time.LocalDate

data class ManglendeInntektsMeldingMelding(
        val organisasjonsnummer: String,
        val fom: LocalDate,
        val tom: LocalDate,
        val fødselsnummer: String,
        val navn: String = ""
)