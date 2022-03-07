package no.nav.helse.spion.koin

import com.zaxxer.hikari.HikariDataSource
import io.ktor.config.ApplicationConfig
import no.nav.helse.arbeidsgiver.bakgrunnsjobb.BakgrunnsjobbRepository
import no.nav.helse.arbeidsgiver.bakgrunnsjobb.BakgrunnsjobbService
import no.nav.helse.arbeidsgiver.bakgrunnsjobb.PostgresBakgrunnsjobbRepository
import no.nav.helse.spion.auth.AuthorizationsRepository
import no.nav.helse.spion.auth.Authorizer
import no.nav.helse.spion.auth.DefaultAuthorizer
import no.nav.helse.spion.auth.DynamicMockAuthRepo
import no.nav.helse.spion.db.createHikariConfig
import no.nav.helse.spion.domene.ytelsesperiode.repository.PostgresYtelsesperiodeRepository
import no.nav.helse.spion.domene.ytelsesperiode.repository.YtelsesperiodeRepository
import no.nav.helse.spion.domenetjenester.SpionService
import no.nav.helse.spion.vedtaksmelding.VedtaksmeldingConsumer
import no.nav.helse.spion.vedtaksmelding.VedtaksmeldingProcessor
import no.nav.helse.spion.vedtaksmelding.VedtaksmeldingProvider
import no.nav.helse.spion.vedtaksmelding.VedtaksmeldingService
import no.nav.helse.spion.web.getString
import no.nav.helse.spion.web.getjdbcUrlFromProperties
import org.koin.dsl.bind
import org.koin.dsl.module
import javax.sql.DataSource

fun preprodConfig(config: ApplicationConfig) = module {
    externalSystemClients(config)

    single {
        HikariDataSource(
            createHikariConfig(
                config.getjdbcUrlFromProperties(),
                config.getString("database.username"),
                config.getString("database.password")

            )
        )
    } bind DataSource::class

    // single { RestStsClientImpl(config.getString("service_user.username"), config.getString("service_user.password"), config.getString("sts_rest_url"), get()) }
    // single { createStaticPdlMock() }
    // single { StaticMockAuthRepo(get()) as AuthorizationsRepository }
    single { DynamicMockAuthRepo(get(), get()) as AuthorizationsRepository }
    single { DefaultAuthorizer(get()) as Authorizer }

    single { createVedtaksMeldingKafkaMock(get()) as VedtaksmeldingProvider }

    single { VedtaksmeldingService(get(), get(), get()) }
    single { VedtaksmeldingConsumer(get(), get(), get()) }
    single { VedtaksmeldingProcessor(get(), get()) }
    single { PostgresYtelsesperiodeRepository(get(), get()) as YtelsesperiodeRepository }
    single { PostgresBakgrunnsjobbRepository(get()) as BakgrunnsjobbRepository }
    single { BakgrunnsjobbService(get()) }

    single { SpionService(get(), get()) }
}
