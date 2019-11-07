package no.nav.helse.spion.web

import com.fasterxml.jackson.core.util.DefaultIndenter
import com.fasterxml.jackson.core.util.DefaultPrettyPrinter
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.KotlinModule
import io.ktor.application.install
import io.ktor.config.ApplicationConfig
import io.ktor.config.MapApplicationConfig
import io.ktor.features.ContentNegotiation
import io.ktor.jackson.jackson
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
import org.slf4j.LoggerFactory
import java.util.concurrent.TimeUnit

val ktorApplicationId = "ktor.application.id"

@KtorExperimentalAPI
fun main() {
    Thread.currentThread().setUncaughtExceptionHandler { thread, err ->
        LoggerFactory.getLogger("main")
            .error("uncaught exception in thread ${thread.name}: ${err.message}", err)
    }
    val config = createConfigFromEnvironment(System.getenv())

    embeddedServer(Netty, createApplicationEnvironment(config)).let { app ->
        app.start(wait = false)

        Runtime.getRuntime().addShutdownHook(Thread {
            app.stop(1, 1, TimeUnit.SECONDS)
        })
    }
}

@KtorExperimentalAPI
fun createApplicationEnvironment(appConfig: ApplicationConfig) = applicationEngineEnvironment {
    config = appConfig

    connector {
        port = 8080
    }

    module {
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
        val dataSource = hikariConfig() //TODO Brukes til å koble opp mot ekte repository senere
        val spionService = SpionService(MockSaksinformasjonRepository())
        routing {
            spion(spionService)
        }
    }
}

@KtorExperimentalAPI
fun createConfigFromEnvironment(env: Map<String, String>) =
    MapApplicationConfig().apply {
        env["DATABASE_HOST"]?.let { put("database.host", it) }
        env["DATABASE_PORT"]?.let { put("database.port", it) }
        env["DATABASE_NAME"]?.let { put("database.name", it) }
        env["DATABASE_USERNAME"]?.let { put("database.username", it) }
        env["DATABASE_PASSWORD"]?.let { put("database.password", it) }

        put("database.jdbc-url", env["DATABASE_JDBC_URL"]
            ?: String.format(
                "jdbc:postgresql://%s:%s/%s%s",
                property("database.host").getString(),
                property("database.port").getString(),
                property("database.name").getString(),
                propertyOrNull("database.username")?.getString()?.let {
                    "?user=$it"
                } ?: ""))

        env["VAULT_MOUNTPATH"]?.let { put("database.vault.mountpath", it) }

        put(ktorApplicationId, env.getOrDefault("KTOR_APPLICATION_ID", "helse-spion"))

    }

