package no.nav.helse.spion.web

import com.typesafe.config.ConfigFactory
import io.ktor.config.HoconApplicationConfig
import io.ktor.server.engine.applicationEngineEnvironment
import io.ktor.server.engine.connector
import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty
import io.ktor.util.KtorExperimentalAPI
import no.nav.helse.arbeidsgiver.bakgrunnsjobb.BakgrunnsjobbService
import no.nav.helse.spion.vedtaksmelding.VedtaksmeldingConsumer
import no.nav.helse.spion.vedtaksmelding.VedtaksmeldingProcessor
import org.koin.ktor.ext.getKoin
import org.slf4j.LoggerFactory


@KtorExperimentalAPI
fun main() {
    Thread.currentThread().setUncaughtExceptionHandler { thread, err ->
        LoggerFactory.getLogger("main")
            .error("uncaught exception in thread ${thread.name}: ${err.message}", err)
    }

    embeddedServer(Netty, createApplicationEnvironment()).let { app ->
        app.start(wait = false)

        val koin = app.application.getKoin()
        val bakgrunnsjobbService = koin.get<BakgrunnsjobbService>()
        bakgrunnsjobbService.leggTilBakgrunnsjobbProsesserer(VedtaksmeldingProcessor.JOBB_TYPE, koin.get<VedtaksmeldingProcessor>())


        val vedtaksmeldingConsumer = koin.get<VedtaksmeldingConsumer>()
        vedtaksmeldingConsumer.startAsync(retryOnFail = true)

        Runtime.getRuntime().addShutdownHook(Thread {
            vedtaksmeldingConsumer.stop()
            app.stop(1000, 1000)
        })
    }
}


@KtorExperimentalAPI
fun createApplicationEnvironment() = applicationEngineEnvironment {
    config = HoconApplicationConfig(ConfigFactory.load())

    connector {
        port = 8080
    }

    module {
        spionModule(config)
    }
}

