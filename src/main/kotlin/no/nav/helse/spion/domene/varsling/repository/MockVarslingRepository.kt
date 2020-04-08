package no.nav.helse.spion.domene.varsling.repository

import java.time.LocalDateTime
import java.util.*

class MockVarslingRepository(): VarslingRepository {

    private val varsling1 = VarslingDbEntity(data="[]", uuid = UUID.randomUUID().toString(), status = false, opprettet = LocalDateTime.now(), virksomhetsNr = "123456789", aggregatperiode = "D-2020-01-01")
    private val varsling2 = VarslingDbEntity(data="[]", uuid = UUID.randomUUID().toString(), status = false, opprettet = LocalDateTime.now(), virksomhetsNr = "123456789", aggregatperiode = "D-2020-01-01")
    private val varsling3 = VarslingDbEntity(data="[]", uuid = UUID.randomUUID().toString(), status = false, opprettet = LocalDateTime.now(), virksomhetsNr = "123456789", aggregatperiode = "D-2020-01-01")

    val list = listOf(varsling1, varsling2, varsling3).toMutableList()
    override fun findByVirksomhetsnummerAndPeriode(virksomhetsnummer: String, aggregatperiode: String): VarslingDbEntity {
        return varsling1
    }

    override fun findByStatus(status: Boolean, max: Int, aggregatPeriode: String): List<VarslingDbEntity> {
        return list
    }

    override fun insert(varsling: VarslingDbEntity) {
        println("Lagret $varsling")
    }

    override fun remove(uuid: String) {
        println("Slettet $uuid")
    }

    override fun updateStatus(uuid: String, timeOfUpdate: LocalDateTime, status: Boolean) {
        println("updateStatus $uuid $status")
    }

    override fun updateData(uuid: String, data: String) {

    }

}