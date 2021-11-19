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
import no.nav.helse.spion.koin.generateEmptyMock
import no.nav.helse.spion.vedtaksmelding.VedtaksmeldingConsumer
import no.nav.helse.spion.vedtaksmelding.VedtaksmeldingProcessor
import no.nav.helse.spion.vedtaksmelding.VedtaksmeldingProvider
import no.nav.helse.spion.vedtaksmelding.VedtaksmeldingService
import org.koin.dsl.bind
import org.koin.dsl.module
import javax.sql.DataSource

fun prodConfig(config: ApplicationConfig) = module {
    single {
        HikariDataSource(
            createHikariConfig(
                config.getjdbcUrlFromProperties(),
                config.getString("database.username"),
                config.getString("database.password")

            )
        )
    } bind DataSource::class

    single { StaticMockAuthRepo(get()) as AuthorizationsRepository } bind StaticMockAuthRepo::class
    single { SpionService(get(), get()) }
    single { DefaultAuthorizer(get()) as Authorizer }

    // single { RestStsClientImpl(config.getString("service_user.username"), config.getString("service_user.password"), config.getString("sts_rest_url"), get()) }
    single { createStaticPdlMock() as PdlClient }
    single { generateEmptyMock() as VedtaksmeldingProvider }

    single { PostgresYtelsesperiodeRepository(get(), get()) as YtelsesperiodeRepository }
    single { PostgresBakgrunnsjobbRepository(get()) as BakgrunnsjobbRepository }
    single { VedtaksmeldingService(get(), get(), get()) }
    single { BakgrunnsjobbService(get()) }

    single { VedtaksmeldingConsumer(get(), get(), get()) }
    single { VedtaksmeldingProcessor(get(), get()) }
}
