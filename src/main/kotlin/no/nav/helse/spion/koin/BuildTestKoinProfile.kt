package no.nav.helse.spion.web

import no.nav.helse.arbeidsgiver.bakgrunnsjobb.BakgrunnsjobbRepository
import no.nav.helse.arbeidsgiver.bakgrunnsjobb.BakgrunnsjobbService
import no.nav.helse.arbeidsgiver.bakgrunnsjobb.MockBakgrunnsjobbRepository
import no.nav.helse.spion.auth.AuthorizationsRepository
import no.nav.helse.spion.auth.Authorizer
import no.nav.helse.spion.auth.DefaultAuthorizer
import no.nav.helse.spion.auth.StaticMockAuthRepo
import no.nav.helse.spion.domene.ytelsesperiode.repository.MockYtelsesperiodeRepository
import no.nav.helse.spion.domene.ytelsesperiode.repository.YtelsesperiodeRepository
import no.nav.helse.spion.domenetjenester.SpionService
import no.nav.helse.spion.koin.createStaticPdlMock
import no.nav.helse.spion.vedtaksmelding.VedtaksmeldingProcessor
import no.nav.helse.spion.vedtaksmelding.VedtaksmeldingService
import org.koin.dsl.bind
import org.koin.dsl.module

fun buildAndTestConfig() = module {
    single { MockYtelsesperiodeRepository() as YtelsesperiodeRepository }
    single { MockBakgrunnsjobbRepository() as BakgrunnsjobbRepository }
    single { StaticMockAuthRepo(get()) as AuthorizationsRepository } bind StaticMockAuthRepo::class
    single { DefaultAuthorizer(get()) as Authorizer }
    single { SpionService(get(), get()) }
    single { VedtaksmeldingService(get(), get(), createStaticPdlMock()) }
    single { VedtaksmeldingProcessor(get(), get()) }
    single { BakgrunnsjobbService(get()) as BakgrunnsjobbService }
}
