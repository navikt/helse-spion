package no.nav.helse.slowtests.db

import com.zaxxer.hikari.HikariDataSource
import no.nav.helse.spion.db.createLocalHikariConfig
import no.nav.helse.spion.domene.varsling.Varsling
import no.nav.helse.spion.domene.varsling.repository.PostgresVarslingRepository
import no.nav.helse.spion.domene.varsling.repository.VarslingDto
import no.nav.helse.spion.web.common
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.koin.core.KoinComponent
import org.koin.core.context.loadKoinModules
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.*

internal class PostgresVarslingRepositoryTest : KoinComponent {

    lateinit var repo: PostgresVarslingRepository
    lateinit var dataSource: HikariDataSource

    private val varsling = Varsling(
            dato = LocalDate.of(2020, 3, 19 ),
            status = 0,
            uuid = "4ded87e3-f266-41b8-8be7-d1c2d037f385",
            liste = mutableSetOf(),
            opprettet = LocalDateTime.of(2020, 3, 19, 22, 30, 44),
            virksomhetsNr = "123456789"
    )

    private val varsling1 = VarslingDto(
            data="[]",
            uuid = UUID.randomUUID().toString(),
            status = 0,
            opprettet = LocalDateTime.now(),
            dato = LocalDate.of(2020,1,1),
            virksomhetsNr = "123456789"
    )

    @BeforeEach
    internal fun setUp() {
        startKoin {
            loadKoinModules(common)
        }
        dataSource = HikariDataSource(createLocalHikariConfig())
        repo = PostgresVarslingRepository(dataSource)

    }

    @AfterEach
    internal fun tearDown() {
        repo.remove("4ded87e3-f266-41b8-8be7-d1c2d037f385")
        stopKoin()
    }


}