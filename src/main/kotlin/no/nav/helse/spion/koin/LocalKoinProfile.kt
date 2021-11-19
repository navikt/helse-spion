package no.nav.helse.spion.web

import com.zaxxer.hikari.HikariDataSource
import io.ktor.config.ApplicationConfig
import no.nav.helse.arbeidsgiver.bakgrunnsjobb.BakgrunnsjobbRepository
import no.nav.helse.arbeidsgiver.bakgrunnsjobb.BakgrunnsjobbService
import no.nav.helse.arbeidsgiver.bakgrunnsjobb.PostgresBakgrunnsjobbRepository
import no.nav.helse.arbeidsgiver.integrasjoner.pdl.PdlClient
import no.nav.helse.spion.auth.AuthorizationsRepository
import no.nav.helse.spion.auth.Authorizer
import no.nav.helse.spion.auth.DefaultAuthorizer
import no.nav.helse.spion.auth.StaticMockAuthRepo
import no.nav.helse.spion.db.createHikariConfig
import no.nav.helse.spion.domene.ytelsesperiode.repository.PostgresYtelsesperiodeRepository
import no.nav.helse.spion.domene.ytelsesperiode.repository.YtelsesperiodeRepository
import no.nav.helse.spion.domenetjenester.SpionService
import no.nav.helse.spion.koin.createStaticPdlMock
import no.nav.helse.spion.koin.createVedtaksMeldingKafkaMock
import no.nav.helse.spion.vedtaksmelding.VedtaksmeldingConsumer
import no.nav.helse.spion.vedtaksmelding.VedtaksmeldingProcessor
import no.nav.helse.spion.vedtaksmelding.VedtaksmeldingProvider
import no.nav.helse.spion.vedtaksmelding.VedtaksmeldingService
import org.koin.dsl.bind
import org.koin.dsl.module
import javax.sql.DataSource

fun localDevConfig(config: ApplicationConfig) = module {
    single {
        HikariDataSource(
            createHikariConfig(
                config.getjdbcUrlFromProperties(),
                config.getString("database.username"),
                config.getString("database.password")

            )
        )
    } bind DataSource::class

    single { PostgresYtelsesperiodeRepository(get(), get()) as YtelsesperiodeRepository }
    single { PostgresBakgrunnsjobbRepository(get()) as BakgrunnsjobbRepository }
    single { StaticMockAuthRepo(get()) as AuthorizationsRepository }
    single { DefaultAuthorizer(get()) as Authorizer }
    single { SpionService(get(), get()) }
    single { BakgrunnsjobbService(get()) }

    single { createVedtaksMeldingKafkaMock(get()) as VedtaksmeldingProvider }

    // single { object : RestStsClient { override fun getOidcToken(): String { return "fake token"} } as RestStsClient }

    single { createStaticPdlMock() as PdlClient }
    single { VedtaksmeldingService(get(), get(), get()) }
    single { VedtaksmeldingConsumer(get(), get(), get()) }
    single { VedtaksmeldingProcessor(get(), get()) }
}
