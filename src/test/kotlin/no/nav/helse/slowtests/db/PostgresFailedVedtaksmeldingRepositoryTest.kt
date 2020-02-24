package no.nav.helse.slowtests.db

import com.zaxxer.hikari.HikariDataSource
import no.nav.helse.spion.db.createLocalHikariConfig
import no.nav.helse.spion.vedtaksmelding.FailedVedtaksmelding
import no.nav.helse.spion.vedtaksmelding.PostgresFailedVedtaksmeldingRepository
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

internal class PostgresFailedVedtaksmeldingRepositoryTest {

    lateinit var repo: PostgresFailedVedtaksmeldingRepository
    lateinit var dataSource: HikariDataSource

    private val failedVedtaksmelding = FailedVedtaksmelding("""{"test": "test"}""", "test")

    @BeforeEach
    internal fun setUp() {
        dataSource = HikariDataSource(createLocalHikariConfig())

        repo = PostgresFailedVedtaksmeldingRepository(dataSource)
    }

    @Test
    fun `Kan lagre, hente og slette (CRuD) og etterlater ingen åpne tilkoblinger fra connectionpoolen`() {
        repo.save(failedVedtaksmelding)

        val messages = repo.getNextFailedMessages(10)

        assertThat(messages.size).isEqualTo(1)
        assertThat(messages.first()).isEqualTo(failedVedtaksmelding)

        repo.delete(failedVedtaksmelding.id)
        val empty = repo.getNextFailedMessages(10)
        assertThat(empty.size).isEqualTo(0)

        assertThat(dataSource.hikariPoolMXBean.activeConnections).isEqualTo(0)
    }
}