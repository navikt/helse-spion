package no.nav.helse.slowtests.db

import com.zaxxer.hikari.HikariDataSource
import no.nav.helse.spion.bakgrunnsjobb.Bakgrunnsjobb
import no.nav.helse.spion.bakgrunnsjobb.BakgrunnsjobbStatus
import no.nav.helse.spion.bakgrunnsjobb.PostgresBakgrunnsjobbRepository
import no.nav.helse.spion.db.createLocalHikariConfig
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.time.LocalDateTime
import java.util.*

internal class PostgresBakgrunnsjobbRepositoryTest {

    lateinit var repo: PostgresBakgrunnsjobbRepository
    lateinit var dataSource: HikariDataSource
    val now = LocalDateTime.now()

    @BeforeEach
    internal fun setUp() {
        dataSource = HikariDataSource(createLocalHikariConfig())
        repo = PostgresBakgrunnsjobbRepository(dataSource)
    }

    @AfterEach
    internal fun cleanUp() {
        repo.deleteAll()
    }

    @Test
    fun `Lagre Les Oppdater Slett`() {
        val uuid = UUID.randomUUID()
        val bakgrunnsjobb = Bakgrunnsjobb(
                uuid,
                "test",
                now,
                now,
                BakgrunnsjobbStatus.OPPRETTET,
                now,
                0,
                3,
                "{}"
        )

        repo.save(bakgrunnsjobb)

        val jobs = repo.findByKjoeretidBeforeAndStatusIn(now.plusHours(1), setOf(BakgrunnsjobbStatus.OPPRETTET))
        assertThat(jobs).hasSize(1)

        val job = jobs.first()
        assertThat(job.uuid).isEqualTo(uuid)
        assertThat(job.type).isEqualTo("test")
        assertThat(job.opprettet).isEqualToIgnoringNanos(now)
        assertThat(job.behandlet).isEqualToIgnoringNanos(now)
        assertThat(job.kjoeretid).isEqualToIgnoringNanos(now)
        assertThat(job.status).isEqualTo(BakgrunnsjobbStatus.OPPRETTET)
        assertThat(job.forsoek).isEqualTo(0)
        assertThat(job.maksAntallForsoek).isEqualTo(3)
        assertThat(job.data).isEqualTo("{}")


        job.status = BakgrunnsjobbStatus.FEILET

        repo.update(job)

        val failedJobs = repo.findByKjoeretidBeforeAndStatusIn(now.plusHours(1), setOf(BakgrunnsjobbStatus.FEILET))
        assertThat(failedJobs).hasSize(1)

        repo.delete(job.uuid)

        val noJobs = repo.findByKjoeretidBeforeAndStatusIn(now.plusHours(1), setOf(BakgrunnsjobbStatus.FEILET))
        assertThat(noJobs).isEmpty()

    }

    @Test
    fun `håndter null`() {
        val uuid = UUID.randomUUID()
        val bakgrunnsjobb = Bakgrunnsjobb(
                uuid,
                "test",
                null,
                now,
                BakgrunnsjobbStatus.OPPRETTET,
                now,
                0,
                3,
                "{}"
        )

        repo.save(bakgrunnsjobb)

        val jobs = repo.findByKjoeretidBeforeAndStatusIn(now.plusHours(1), setOf(BakgrunnsjobbStatus.OPPRETTET))
        assertThat(jobs).hasSize(1)
        assertThat(jobs.first().behandlet).isNull()

    }


}
