package no.nav.helse.spion.domene.varsling.repository

import java.sql.ResultSet

fun mapDto(res: ResultSet): VarslingDto {
    return VarslingDto(
            data = res.getString("data"),
            uuid = res.getString("uuid"),
            status = res.getInt("status"),
            opprettet = res.getTimestamp("opprettet").toLocalDateTime(),
            behandlet = res.getTimestamp("behandlet")?.toLocalDateTime(),
            dato = res.getDate("dato").toLocalDate(),
            virksomhetsNr = res.getString("virksomhetsNr")
    )
}