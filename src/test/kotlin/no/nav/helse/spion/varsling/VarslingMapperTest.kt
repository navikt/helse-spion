package no.nav.helse.spion.varsling

import com.fasterxml.jackson.core.util.DefaultIndenter
import com.fasterxml.jackson.core.util.DefaultPrettyPrinter
import com.fasterxml.jackson.databind.MapperFeature
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.KotlinModule
import no.nav.helse.spion.domene.Periode
import no.nav.helse.spion.domene.varsling.PersonVarsling
import no.nav.helse.spion.domene.varsling.Varsling
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.time.LocalDateTime

internal class VarslingMapperTest {

    @Test
    fun `Kan mappe fra domene objekt til dto og tilbake`() {

        val om = ObjectMapper()
        om.registerModule(KotlinModule())
        om.registerModule(Jdk8Module())
        om.registerModule(JavaTimeModule())
        om.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
        om.configure(SerializationFeature.INDENT_OUTPUT, true)
        om.configure(MapperFeature.ACCEPT_CASE_INSENSITIVE_PROPERTIES, true)

        om.setDefaultPrettyPrinter(DefaultPrettyPrinter().apply {
            indentArraysWith(DefaultPrettyPrinter.FixedSpaceIndenter.instance)
            indentObjectsWith(DefaultIndenter("  ", "\n"))
        })
        val mapper = VarslingMapper(om)

        val periode = Periode(
                LocalDate.of(2020,3,6),
                LocalDate.of(2020,3,23)
        )
        val pv1 = PersonVarsling("Ola Nordmann", "987654321", periode)
        val pv2 = PersonVarsling("Kari Nordmann", "123456789", periode)
        val varsling = Varsling(
                uuid = "123",
                status = 1,
                virksomhetsNr = "123456789",
                opprettet = LocalDateTime.of(2019,3, 23, 0,0,0),
                dato = LocalDate.of(2020,3, 23),
                liste = setOf(pv1, pv2).toMutableSet()
        )
        val dto = mapper.mapDto(varsling)
        assertThat(dto.uuid).isEqualTo("123")
        assertThat(dto.dato).isEqualTo(LocalDate.of(2020,3, 23))
        assertThat(dto.status).isEqualTo(1)
        assertThat(dto.virksomhetsNr).isEqualTo("123456789")
        assertThat(dto.opprettet).isEqualTo(LocalDateTime.of(2019,3, 23, 0,0,0))

        val domain = mapper.mapDomain(dto)
        assertThat(domain.status).isEqualTo(1)
        val perioder = domain.liste
        assertThat(perioder.size).isEqualTo(2)



        val p1 = perioder.stream().filter{ it.personnumer == "987654321" }.findFirst().get()
        val p2 = perioder.stream().filter{ it.personnumer == "123456789" }.findFirst().get()
        assertThat(p1.navn).isEqualTo("Ola Nordmann")
        assertThat(p1.personnumer).isEqualTo("987654321")
        assertThat(p1.periode.fom).isEqualTo(LocalDate.of(2020,3,6))
        assertThat(p1.periode.tom).isEqualTo(LocalDate.of(2020,3,23))

        assertThat(p2.navn).isEqualTo("Kari Nordmann")
        assertThat(p2.personnumer).isEqualTo("123456789")
        assertThat(p2.periode.fom).isEqualTo(LocalDate.of(2020,3,6))
        assertThat(p2.periode.tom).isEqualTo(LocalDate.of(2020,3,23))
    }

}