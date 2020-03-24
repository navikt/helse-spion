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
            uuid = "4ded87e3-f266-41b8-8be7-d1c2d037f385",
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
        repository.save(varsling.copy(uuid = "5ded87e3-f266-41b8-8be7-d1c2d037f385"))
        repository.save(varsling.copy(uuid = "6ded87e3-f266-41b8-8be7-d1c2d037f385"))
        repository.save(varsling.copy(uuid = "7ded87e3-f266-41b8-8be7-d1c2d037f385", status = 1))
        val list1 = repository.findByStatus(LocalDate.of(2020, 3, 19 ), 0, 10)
        assertThat(list1.size).isEqualTo(2)
        repository.save(varsling.copy( status = 1, behandlet = LocalDateTime.of(2020, 3, 19, 22, 30, 44)))
        val list2 = repository.findByStatus(LocalDate.of(2020, 3, 19 ), 1, 10)
        assertThat(list2.size).isEqualTo(1)
        val dto = list2[0]
        assertThat(dto.status).isEqualTo(1)
        assertThat(dto.behandlet).isEqualTo(LocalDateTime.of(2020, 3, 19, 22, 30, 44))
        repository.remove("5ded87e3-f266-41b8-8be7-d1c2d037f385")
        repository.remove("6ded87e3-f266-41b8-8be7-d1c2d037f385")
        repository.remove("7ded87e3-f266-41b8-8be7-d1c2d037f385")
        val list3 = repository.findByStatus(LocalDate.of(2020, 3, 19 ), 1, 10)
        assertThat(list3.size).isEqualTo(0)
        assertThat(dataSource.hikariPoolMXBean.activeConnections).isEqualTo(0)
    }

}