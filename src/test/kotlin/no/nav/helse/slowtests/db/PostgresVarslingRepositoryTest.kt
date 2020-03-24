package no.nav.helse.slowtests.db

import com.zaxxer.hikari.HikariDataSource
import no.nav.helse.spion.db.createLocalHikariConfig
import no.nav.helse.spion.domene.varsling.repository.PostgresVarslingRepository
import no.nav.helse.spion.domene.varsling.repository.VarslingDto
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.koin.core.KoinComponent
import java.time.LocalDate
import java.time.LocalDateTime

internal class PostgresVarslingRepositoryTest : KoinComponent {

    lateinit var repository: PostgresVarslingRepository
    lateinit var dataSource: HikariDataSource

    private val varsling = VarslingDto(
            dato = LocalDate.of(2020, 3, 19 ),
            status = 0,
            uuid = "mangler",
            data = "[]",
            opprettet = LocalDateTime.of(2019, 3, 19, 22, 30, 44),
            virksomhetsNr = "123456789"
    )

    @BeforeEach
    internal fun setUp() {
        dataSource = HikariDataSource(createLocalHikariConfig())
        repository = PostgresVarslingRepository(dataSource)
    }

    @Test
    fun `Kan lagre, hente og slette (CRuD) og etterlater ingen åpne tilkoblinger fra connectionpoolen`() {
        // Opprett 3 stk
        repository.save(varsling.copy(uuid = "1ded87e3-f266-41b8-8be7-d1c2d037f385"))
        repository.save(varsling.copy(uuid = "2ded87e3-f266-41b8-8be7-d1c2d037f385"))
        repository.save(varsling.copy(uuid = "3ded87e3-f266-41b8-8be7-d1c2d037f385", status = 1))
        val listStatusNull = repository.findByStatus(LocalDate.of(2020, 3, 19 ), 0, 10)
        // Finn kun to stk da en er med annen status = 0
        assertThat(listStatusNull.size).isEqualTo(2)
        assertThat(listStatusNull[0].uuid).isEqualTo("1ded87e3-f266-41b8-8be7-d1c2d037f385") // Skal være riktig sortert
        assertThat(listStatusNull[1].uuid).isEqualTo("2ded87e3-f266-41b8-8be7-d1c2d037f385") //

        // Lagre 1 med status = 1
        repository.save(varsling.copy(uuid = "4ded87e3-f266-41b8-8be7-d1c2d037f385", status = 1, dato = LocalDate.of(2020, 4, 19), behandlet = LocalDateTime.of(2020,4, 20,14,0,0)))
        val listStatusEn = repository.findByStatus(LocalDate.of(2020, 4, 19 ), 1, 10)
        assertThat(listStatusEn.size).isEqualTo(1)

        val dto = listStatusEn[0]
        assertThat(dto.status).isEqualTo(1)
        assertThat(dto.uuid).isEqualTo("4ded87e3-f266-41b8-8be7-d1c2d037f385")
        assertThat(dto.behandlet).isEqualTo(LocalDateTime.of(2020,4, 20,14,0,0))
        repository.remove("1ded87e3-f266-41b8-8be7-d1c2d037f385")
        repository.remove("2ded87e3-f266-41b8-8be7-d1c2d037f385")
        repository.remove("3ded87e3-f266-41b8-8be7-d1c2d037f385")
        repository.remove("4ded87e3-f266-41b8-8be7-d1c2d037f385")
        val list3 = repository.findByStatus(LocalDate.of(2020, 3, 19 ), 1, 10)
        assertThat(list3.size).isEqualTo(0)
        assertThat(dataSource.hikariPoolMXBean.activeConnections).isEqualTo(0)
    }

}