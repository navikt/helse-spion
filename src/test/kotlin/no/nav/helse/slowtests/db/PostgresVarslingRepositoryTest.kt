package no.nav.helse.slowtests.db

import com.fasterxml.jackson.databind.ObjectMapper
import com.zaxxer.hikari.HikariDataSource
import no.nav.helse.spion.db.createLocalHikariConfig
import no.nav.helse.spion.domene.varsling.Varsling
import no.nav.helse.spion.domene.varsling.repository.PostgresVarslingRepository
import no.nav.helse.spion.web.common
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.koin.core.KoinComponent
import org.koin.core.context.loadKoinModules
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import org.koin.core.get
import java.time.LocalDate
import java.time.LocalDateTime

internal class PostgresVarslingRepositoryTest : KoinComponent {

    lateinit var repo: PostgresVarslingRepository
    lateinit var dataSource: HikariDataSource

    private val varsling = Varsling(
            dato = LocalDate.of(2020, 3, 19 ),
            status = 0,
            uuid = "4ded87e3-f266-41b8-8be7-d1c2d037f385",
            liste = listOf(),
            opprettet = LocalDateTime.of(2020, 3, 19, 22, 30, 44),
            virksomhetsNr = "123456789"
    )

    @BeforeEach
    internal fun setUp() {
        startKoin {
            loadKoinModules(common)
        }
        dataSource = HikariDataSource(createLocalHikariConfig())
        val objectMapper = get<ObjectMapper>()
        repo = PostgresVarslingRepository(dataSource, objectMapper)

    }

    @AfterEach
    internal fun tearDown() {
        repo.remove("4ded87e3-f266-41b8-8be7-d1c2d037f385")
        stopKoin()
    }

    @Test
    fun finnNesteUbehandlet() {
        repo.insert(varsling)
        val neste = repo.findByStatus()
        assertThat(neste.uuid).isEqualTo("4ded87e3-f266-41b8-8be7-d1c2d037f385")
        repo.remove("4ded87e3-f266-41b8-8be7-d1c2d037f385")
    }

    @Test
    fun finnAntallUbehandlet() {
        assertThat(repo.countByStatus()).isEqualTo(1)
    }

    @Test
    fun oppdaterStatus() {
        val neste = repo.findByStatus()
        repo.oppdaterStatus(neste, true)
        val lagret = repo.findByStatus()
        assertThat(lagret.status).isEqualTo(1)
    }

//    @Test
//    fun mapToDto() {
//    }
//
//    @Test
//    fun testMapToDto() {
//    }
//
//    @Test
//    fun mapToDomain() {
//    }

}