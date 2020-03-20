package no.nav.helse.spion.varsling

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import no.nav.helse.spion.domene.varsling.Varsling
import no.nav.helse.spion.domene.varsling.repository.VarslingDto

class VarslingMapper (private val mapper: ObjectMapper) {

    fun mapDto(varsling: Varsling): VarslingDto {
        return VarslingDto(
                uuid = varsling.uuid,
                data = mapper.writeValueAsString(varsling.liste),
                status = varsling.status,
                opprettet = varsling.opprettet,
                dato = varsling.dato,
                virksomhetsNr = varsling.virksomhetsNr
        )
    }

    fun mapDomain(dto: VarslingDto): Varsling {
        return Varsling(
                dato = dto.dato,
                virksomhetsNr = dto.virksomhetsNr,
                uuid = dto.uuid,
                opprettet = dto.opprettet,
                status = dto.status,
                liste = mapper.readValue(dto.data)
        )
    }

}