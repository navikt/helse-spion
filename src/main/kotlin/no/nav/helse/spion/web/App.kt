package no.nav.helse.spion.web

import com.fasterxml.jackson.core.util.DefaultIndenter
import com.fasterxml.jackson.core.util.DefaultPrettyPrinter
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.KotlinModule
import com.typesafe.config.ConfigFactory
import io.ktor.application.Application
import io.ktor.application.call
import io.ktor.application.install
import io.ktor.config.ApplicationConfig
import io.ktor.config.MapApplicationConfig
import io.ktor.features.ContentNegotiation
import io.ktor.jackson.jackson
import io.ktor.auth.Authentication
import io.ktor.auth.authenticate
import io.ktor.config.HoconApplicationConfig
import io.ktor.http.ContentType
import io.ktor.response.respondText
import io.ktor.routing.get
import io.ktor.routing.routing
import io.ktor.server.engine.applicationEngineEnvironment
import io.ktor.server.engine.connector
import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty
import io.ktor.util.KtorExperimentalAPI
import no.nav.helse.spion.db.hikariConfig
import no.nav.helse.spion.domene.saksinformasjon.repository.MockSaksinformasjonRepository
import no.nav.helse.spion.domenetjenester.SpionService
import no.nav.helse.spion.nais.nais
import no.nav.helse.spion.web.api.spion
import no.nav.security.token.support.ktor.tokenValidationSupport
import org.slf4j.LoggerFactory
import java.util.concurrent.TimeUnit

@KtorExperimentalAPI
fun main() {
    Thread.currentThread().setUncaughtExceptionHandler { thread, err ->
        LoggerFactory.getLogger("main")
            .error("uncaught exception in thread ${thread.name}: ${err.message}", err)
    }
    val config = HoconApplicationConfig(ConfigFactory.load())

    embeddedServer(Netty, createApplicationEnvironment()).let { app ->

        app.start(wait = false)

        Runtime.getRuntime().addShutdownHook(Thread {
            app.stop(1, 1, TimeUnit.SECONDS)
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

@KtorExperimentalAPI
fun Application.spionModule(config : ApplicationConfig) {
    install(Authentication) {
        tokenValidationSupport(config = config)
    }

    install(ContentNegotiation) {
        jackson() {
            this.registerModule(KotlinModule())
            this.registerModule(Jdk8Module())
            this.registerModule(JavaTimeModule())
            this.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
            configure(SerializationFeature.INDENT_OUTPUT, true)
            setDefaultPrettyPrinter(DefaultPrettyPrinter().apply {
                indentArraysWith(DefaultPrettyPrinter.FixedSpaceIndenter.instance)
                indentObjectsWith(DefaultIndenter("  ", "\n"))
            })
        }
    }

    nais()

    //val dataSource = hikariConfig() //TODO vil brukes til å koble opp mot ekte repository senere
    val spionService = SpionService(MockSaksinformasjonRepository())
    routing {
        spion(spionService)
    }
}

