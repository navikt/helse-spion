package no.nav.helse.spion.domene.varsling

import java.time.LocalDate
import java.time.LocalDateTime
import java.util.*

data class PersonVarsling(val navn: String, val personnumer: String)

data class Varsling(
        val dato: LocalDate,
        val virksomhetsNr: String,
        val liste: List<PersonVarsling>,
        val uuid: String = UUID.randomUUID().toString(),
        val opprettet: LocalDateTime = LocalDateTime.now(),
        val status: Int = 0
)