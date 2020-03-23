package no.nav.helse.spion.domene.varsling.repository

import java.time.LocalDate
import java.time.LocalDateTime

data class VarslingDto(
        val data: String,
        val uuid: String,
        val status: Int,
        val opprettet: LocalDateTime,
        val behandlet: LocalDateTime? = null,
        val dato: LocalDate,
        val virksomhetsNr: String
)