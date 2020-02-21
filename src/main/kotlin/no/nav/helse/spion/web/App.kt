package no.nav.helse.spion.web

import com.fasterxml.jackson.databind.ObjectMapper
import com.typesafe.config.ConfigFactory
import io.ktor.application.Application
import io.ktor.application.install
import io.ktor.auth.Authentication
import io.ktor.auth.authenticate
import io.ktor.config.ApplicationConfig
import io.ktor.config.HoconApplicationConfig
import io.ktor.features.ContentNegotiation
import io.ktor.features.DoubleReceive
import io.ktor.http.ContentType
import io.ktor.jackson.JacksonConverter
import io.ktor.routing.routing
import io.ktor.server.engine.applicationEngineEnvironment
import io.ktor.server.engine.connector
import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty
import io.ktor.util.KtorExperimentalAPI
import no.nav.helse.spion.auth.localCookieDispenser
import no.nav.helse.spion.nais.nais
import no.nav.helse.spion.vedtaksmelding.VedtaksmeldingProcessor
import no.nav.helse.spion.web.api.spion
import no.nav.security.token.support.ktor.tokenValidationSupport
import org.koin.ktor.ext.Koin
import org.koin.ktor.ext.get
import org.koin.ktor.ext.getKoin
import org.slf4j.LoggerFactory
import java.util.concurrent.TimeUnit

@KtorExperimentalAPI
fun main() {
    Thread.currentThread().setUncaughtExceptionHandler { thread, err ->
        LoggerFactory.getLogger("main")
            .error("uncaught exception in thread ${thread.name}: ${err.message}", err)
    }

    embeddedServer(Netty, createApplicationEnvironment()).let { app ->
        app.start(wait = false)
        Runtime.getRuntime().addShutdownHook(Thread {
            app.stop(1, 1, TimeUnit.SECONDS)
        })

        val kafkaProcessor = app.application.getKoin().get<VedtaksmeldingProcessor>()
        kafkaProcessor.startAsync()
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

@KtorExperimentalAPI
fun Application.spionModule(config : ApplicationConfig = environment.config) {
    install(Koin) {
        modules(selectModuleBasedOnProfile(config))
    }

    install(Authentication) {
        tokenValidationSupport(config = config)
    }

    install(DoubleReceive)

    install(ContentNegotiation) {
        val commonObjectMapper = get<ObjectMapper>()
        register(ContentType.Application.Json, JacksonConverter(commonObjectMapper))
    }

    nais()
    localCookieDispenser(config)

    routing {
        authenticate {
            spion(get(), get())
        }
    }
}