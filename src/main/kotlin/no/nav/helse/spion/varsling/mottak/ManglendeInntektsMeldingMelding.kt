package no.nav.helse.spion.varsling.mottak

import java.time.LocalDate

data class ManglendeInntektsMeldingMelding(
    val virksomhetsnummer: String,
    val fom: LocalDate,
    val tom: LocalDate,
    val identitetsnummer: String,
    val navn: String,
    val refusjon: Boolean
)