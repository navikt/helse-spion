package no.nav.helse.spion.varsling

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import no.nav.helse.spion.domene.varsling.Varsling
import no.nav.helse.spion.domene.varsling.repository.VarslingDbEntity

class VarslingMapper (private val mapper: ObjectMapper) {

    fun mapDto(varsling: Varsling): VarslingDbEntity {
        return VarslingDbEntity(
                uuid = varsling.uuid,
                data = mapper.writeValueAsString(varsling.liste),
                status = varsling.varslingSendt,
                opprettet = varsling.opprettet,
                aggregatperiode = varsling.aggregatperiode,
                virksomhetsNr = varsling.virksomhetsNr
        )
    }

    fun mapDomain(dbEntity: VarslingDbEntity): Varsling {
        return Varsling(
                aggregatperiode = dbEntity.aggregatperiode,
                virksomhetsNr = dbEntity.virksomhetsNr,
                uuid = dbEntity.uuid,
                opprettet = dbEntity.opprettet,
                varslingSendt = dbEntity.status,
                liste = mapper.readValue(dbEntity.data)
        )
    }

}