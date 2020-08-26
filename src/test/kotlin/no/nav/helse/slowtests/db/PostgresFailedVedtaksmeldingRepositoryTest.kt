package no.nav.helse.slowtests.db

import com.fasterxml.jackson.databind.ObjectMapper
import com.zaxxer.hikari.HikariDataSource
import no.nav.helse.KoinInjectedTest
import no.nav.helse.spion.db.createLocalHikariConfig
import no.nav.helse.spion.vedtaksmelding.SpleisVedtaksmeldingGenerator
import no.nav.helse.spion.vedtaksmelding.failed.FailedVedtaksmelding
import no.nav.helse.spion.vedtaksmelding.failed.PostgresFailedVedtaksmeldingRepository
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

internal class PostgresFailedVedtaksmeldingRepositoryTest : KoinInjectedTest() {

    lateinit var repo: PostgresFailedVedtaksmeldingRepository
    lateinit var dataSource: HikariDataSource
    val meldingsGenerator = SpleisVedtaksmeldingGenerator(maxUniqueArbeidsgivere = 10, maxUniquePersoner = 10)

    private val failedVedtaksmelding = FailedVedtaksmelding(meldingsGenerator.next(), "test feil")

    @BeforeEach
    internal fun setUp() {
        dataSource = HikariDataSource(createLocalHikariConfig())
        val om = koin.get<ObjectMapper>()

        repo = PostgresFailedVedtaksmeldingRepository(dataSource, om)
    }

    @AfterEach
    internal fun cleanUp() {
        repo.delete(failedVedtaksmelding.id)
    }

    @Test
    fun `Kan lagre, hente og slette (CRuD) og etterlater ingen åpne tilkoblinger fra connectionpoolen`() {
        repo.save(failedVedtaksmelding)

        val messages = repo.getFailedMessages(10)

        assertThat(messages.size).isEqualTo(1)
        assertThat(messages.first()).isEqualTo(failedVedtaksmelding)

        repo.delete(failedVedtaksmelding.id)
        val empty = repo.getFailedMessages(10)
        assertThat(empty.size).isEqualTo(0)

        assertThat(dataSource.hikariPoolMXBean.activeConnections).isEqualTo(0)
    }
}