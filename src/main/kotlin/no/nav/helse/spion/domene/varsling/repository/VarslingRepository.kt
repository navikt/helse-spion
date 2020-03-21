package no.nav.helse.spion.domene.varsling.repository

import java.time.LocalDateTime

interface VarslingRepository {
    fun findByStatus(status: Int, max: Int) : List<VarslingDto>
    fun countByStatus(status: Int) : Int
    fun update(varsling: VarslingDto)
    fun insert(varsling: VarslingDto)
    fun remove(uuid: String)
    fun updateStatus(uuid: String, dato: LocalDateTime, status: Int)
}