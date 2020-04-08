package no.nav.helse.spion.domene.varsling.repository

import java.time.LocalDateTime

interface VarslingRepository {

    // For å hente ut aggregat og aggregere viderer på det
    fun findByVirksomhetsnummerAndPeriode(virksomhetsnummer: String, aggregatperiode: String): VarslingDbEntity?

    // for å hente ut alle aggregat i en gitt status i en gitt periode
    fun findByStatus(status: Boolean, max: Int, aggregatPeriode: String) : List<VarslingDbEntity>

    // sette inn nytt aggregat
    fun insert(varsling: VarslingDbEntity)

    fun remove(uuid: String)

    // for å sette status til sendt når melding for aggregatet er sendt
    fun updateStatus(uuid: String, timeOfUpdate: LocalDateTime, status: Boolean)
    fun updateData(uuid: String, data: String)
}