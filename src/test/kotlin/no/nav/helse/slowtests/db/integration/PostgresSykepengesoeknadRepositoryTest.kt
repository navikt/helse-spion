package no.nav.helse.slowtests.db.integration

import com.zaxxer.hikari.HikariDataSource
import no.nav.helse.spion.db.createLocalHikariConfig
import no.nav.helse.spion.domene.Periode
import no.nav.helse.spion.domene.sykepengesoeknad.PostgresSykepengesoeknadRepository
import no.nav.helse.spion.domene.sykepengesoeknad.Sykepengesoeknad
import no.nav.helse.spion.web.common
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.koin.core.KoinComponent
import org.koin.core.context.loadKoinModules
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import java.time.LocalDate
import java.util.*

internal class PostgresSykepengesoeknadRepositoryTest : KoinComponent {

    lateinit var repo: PostgresSykepengesoeknadRepository;
    val testSykepengesoeknad = Sykepengesoeknad(UUID.randomUUID(), Periode(LocalDate.MIN, LocalDate.MAX))

    @BeforeEach
    internal fun setUp() {
        startKoin {
            loadKoinModules(common)

        }
        repo = PostgresSykepengesoeknadRepository(HikariDataSource(createLocalHikariConfig()))
        repo.upsert(testSykepengesoeknad)
    }

    @AfterEach
    internal fun tearDown() {
        repo.delete(testSykepengesoeknad.uuid)
        stopKoin()
    }

    @Test
    fun `Henter en søknad fra repo`() {
        val fraDb = repo.getById(testSykepengesoeknad.uuid)

        assertThat(fraDb).isNotNull()
        assertThat(fraDb).isEqualTo(testSykepengesoeknad)
    }
    
    @Test
    fun `Sletter en søkenad`() {
        val deletedCount = repo.delete(testSykepengesoeknad.uuid)
        assertThat(deletedCount).isEqualTo(1)
    }

    @Test
    fun `samme id oppdaterer fom og tom`() {
        val updated = testSykepengesoeknad.copy(
                periode = Periode(LocalDate.of(2000, 1, 1), LocalDate.now()))

        repo.upsert(updated)

        val fromDb = repo.getById(testSykepengesoeknad.uuid)
        assertThat(fromDb).isEqualTo(updated)

    }
}