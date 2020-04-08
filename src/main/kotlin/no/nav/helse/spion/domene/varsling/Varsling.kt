package no.nav.helse.spion.domene.varsling

import no.nav.helse.spion.domene.Periode
import java.time.LocalDateTime
import java.util.*

data class PersonVarsling(
        val navn: String,
        val personnumer: String,
        val periode: Periode,
        val varselOpprettet: LocalDateTime
)

data class Varsling(
        val aggregatperiode: String,
        val virksomhetsNr: String,
        val liste: MutableSet<PersonVarsling>,
        val uuid: String = UUID.randomUUID().toString(), // Uuid sendes også til Altinn som referanse
        val opprettet: LocalDateTime = LocalDateTime.now(),
        val varslingSendt: Boolean = false
)