package no.nav.helse.spion.domene.varsling.repository

import java.time.LocalDateTime

data class VarslingDbEntity(
        val data: String,
        val uuid: String,
        val status: Boolean,
        val opprettet: LocalDateTime,
        val behandlet: LocalDateTime? = null,
        val aggregatperiode: String,
        val virksomhetsNr: String
)