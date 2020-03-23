package no.nav.helse.spion.domene.varsling

import no.nav.helse.spion.domene.Periode
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.*

data class PersonVarsling(val navn: String, val personnumer: String, val periode: Periode)

data class Varsling(
        val dato: LocalDate,
        val virksomhetsNr: String,
        val liste: MutableSet<PersonVarsling>,
        val uuid: String = UUID.randomUUID().toString(), // Uuid sendes til Altinn som referanse
        val opprettet: LocalDateTime = LocalDateTime.now(),
        val status: Int = 0
)

enum class Status {
    Venter, OK, Feilet
}