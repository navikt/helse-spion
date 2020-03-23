package no.nav.helse.spion.domene.varsling.repository

import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.sql.Date
import java.sql.ResultSet
import java.sql.Timestamp
import java.time.LocalDate
import java.time.LocalDateTime

internal class VarslingMapperKtTest {

    @Test
    fun `Skal mappe ikke påkrevde felter og til slutt samtlige felter`(){
        val rs = mockk<ResultSet>()
        every { rs.getString("data") } returns "[]"
        every { rs.getString("uuid") } returns "1234-abcd-5678"
        every { rs.getInt("status") } returns 1
        every { rs.getTimestamp("opprettet") } returns Timestamp.valueOf(LocalDateTime.of(2019,3,23,9,31,0))
        every { rs.getDate("dato") } returns Date.valueOf(LocalDate.of(2020,3,23))
        every { rs.getString("virksomhetsNr") } returns "123456789"
        every { rs.getTimestamp("behandlet") } returns null
        val dto = mapDto( rs )
        assertThat(dto.data).isEqualTo("[]")
        assertThat(dto.uuid).isEqualTo("1234-abcd-5678")
        assertThat(dto.status).isEqualTo(1)
        assertThat(dto.opprettet).isEqualTo(LocalDateTime.of(2019,3,23,9,31,0,0))
        assertThat(dto.dato).isEqualTo(LocalDate.of(2020,3,23))
        assertThat(dto.virksomhetsNr).isEqualTo("123456789")
        every { rs.getTimestamp("behandlet") } returns Timestamp.valueOf(LocalDateTime.of(2020,3,23,9,31,0))
        val dto2 = mapDto( rs )
        assertThat (dto2.behandlet).isEqualTo(LocalDateTime.of(2020,3,23,9,31,0))
    }
}