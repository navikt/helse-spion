package no.nav.helse.spion.varsling.mottak

import java.time.LocalDate
import java.time.LocalDateTime

data class ManglendeInntektsMeldingMelding(
        val organisasjonsnummer: String,
        val fom: LocalDate,
        val tom: LocalDate,
        val opprettet: LocalDateTime, // Dette er tidspunktet da vedtakssystemet ville ha en inntektsmelding
        val fødselsnummer: String,
        val navn: String = ""
)