package no.nav.helse.spion.domene.varsling.repository

import java.time.LocalDate
import java.time.LocalDateTime

interface VarslingRepository {
    fun findByVirksomhetsnummerAndDato(virksomhetsnummer: String, dato: LocalDate): VarslingDto?
    fun findByStatus(dato: LocalDate, status: Int, max: Int) : List<VarslingDto>
    fun countByStatus(dato: LocalDate, status: Int) : Int
    fun update(varsling: VarslingDto)
    fun save(varsling: VarslingDto)
    fun remove(uuid: String)
    fun updateStatus(uuid: String, dato: LocalDateTime, status: Int)
}