package no.nav.helse.spion.web

import com.typesafe.config.ConfigFactory
import io.ktor.config.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import io.ktor.util.*
import no.nav.helse.spion.vedtaksmelding.VedtaksmeldingProcessor
import no.nav.helse.spion.vedtaksmelding.failed.FailedVedtaksmeldingProcessor
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
        val vedtaksmeldingProcessor = koin.get<VedtaksmeldingProcessor>()
        vedtaksmeldingProcessor.startAsync(retryOnFail = true)

        val failedVedtaksmeldingProcessor = koin.get<FailedVedtaksmeldingProcessor>()
        //failedVedtaksmeldingProcessor.startAsync(retryOnFail = true)

        Runtime.getRuntime().addShutdownHook(Thread {
            vedtaksmeldingProcessor.stop()
            failedVedtaksmeldingProcessor.stop()
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

