package no.nav.helse.spion.domene.varsling.repository

import java.time.LocalDate
import java.time.LocalDateTime
import java.util.*

class MockVarslingRepository(): VarslingRepository {

    private val varsling1 = VarslingDto(data="[]", uuid = UUID.randomUUID().toString(), status = 0, opprettet = LocalDateTime.now(), dato = LocalDate.of(2020,1,1), virksomhetsNr = "123456789" )
    private val varsling2 = VarslingDto(data="[]", uuid = UUID.randomUUID().toString(), status = 0, opprettet = LocalDateTime.now(), dato = LocalDate.of(2020,1,1), virksomhetsNr = "123456789" )
    private val varsling3 = VarslingDto(data="[]", uuid = UUID.randomUUID().toString(), status = 0, opprettet = LocalDateTime.now(), dato = LocalDate.of(2020,1,1), virksomhetsNr = "123456789" )

    val list = listOf(varsling1, varsling2, varsling3).toMutableList()

    override fun findByStatus(status: Int, max: Int): List<VarslingDto> {
        return list
    }

    override fun countByStatus(status: Int): Int {
        return list.size
    }

    override fun update(varsling: VarslingDto) {
        println("Oppdater $varsling.uuid")
    }

    override fun insert(varsling: VarslingDto) {
        println("Lagret $varsling")
    }

    override fun remove(uuid: String) {
        println("Slettet $uuid")
    }

    override fun updateStatus(uuid: String, dato: LocalDateTime, status: Int) {
        println("updateStatus $uuid $status")
    }

}