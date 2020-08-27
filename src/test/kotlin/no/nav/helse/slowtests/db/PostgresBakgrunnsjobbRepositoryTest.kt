package no.nav.helse.slowtests.db

import com.zaxxer.hikari.HikariDataSource
import no.nav.helse.spion.bakgrunnsjobb.Bakgrunnsjobb
import no.nav.helse.spion.bakgrunnsjobb.BakgrunnsjobbStatus
import no.nav.helse.spion.bakgrunnsjobb.PostgresBakgrunnsjobbRepository
import no.nav.helse.spion.db.createLocalHikariConfig
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.time.LocalDateTime
import java.util.*

internal class PostgresBakgrunnsjobbRepositoryTest {

    lateinit var repo: PostgresBakgrunnsjobbRepository
    lateinit var dataSource: HikariDataSource

    @BeforeEach
    internal fun setUp() {
        dataSource = HikariDataSource(createLocalHikariConfig())

        repo = PostgresBakgrunnsjobbRepository(dataSource)
    }

    @Test
    fun `lagre og hente opp igjen bakgrunnsjobb`() {
        val bakgrunnsjobb = Bakgrunnsjobb(
                UUID.randomUUID(),
                "test",
                LocalDateTime.now(),
                LocalDateTime.now(),
                BakgrunnsjobbStatus.OPPRETTET,
                LocalDateTime.now(),
                0,
                3,
                "{}"
        )

        repo.save(bakgrunnsjobb)

        val jobs = repo.findByKjoeretidBeforeAndStatusIn(LocalDateTime.now().plusHours(1), setOf(BakgrunnsjobbStatus.OPPRETTET))
        assertThat(jobs).hasSize(1)
    }
}
