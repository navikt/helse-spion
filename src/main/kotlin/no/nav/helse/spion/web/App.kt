package no.nav.helse.spion.web

import com.fasterxml.jackson.databind.JsonMappingException
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.MissingKotlinParameterException
import com.typesafe.config.ConfigFactory
import io.ktor.application.Application
import io.ktor.application.call
import io.ktor.application.install
import io.ktor.auth.Authentication
import io.ktor.auth.authenticate
import io.ktor.config.ApplicationConfig
import io.ktor.config.HoconApplicationConfig
import io.ktor.features.ContentNegotiation
import io.ktor.features.DoubleReceive
import io.ktor.features.StatusPages
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.jackson.JacksonConverter
import io.ktor.response.respond
import io.ktor.routing.routing
import io.ktor.server.engine.applicationEngineEnvironment
import io.ktor.server.engine.connector
import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty
import io.ktor.util.KtorExperimentalAPI
import no.nav.helse.spion.auth.localCookieDispenser
import no.nav.helse.spion.nais.nais
import no.nav.helse.spion.vedtaksmelding.VedtaksmeldingProcessor
import no.nav.helse.spion.vedtaksmelding.failed.FailedVedtaksmeldingProcessor
import no.nav.helse.spion.web.api.spion
import no.nav.helse.spion.web.dto.validation.Problem
import no.nav.helse.spion.web.dto.validation.ValidationProblem
import no.nav.helse.spion.web.dto.validation.ValidationProblemDetail
import no.nav.security.token.support.ktor.tokenValidationSupport
import org.koin.ktor.ext.Koin
import org.koin.ktor.ext.get
import org.koin.ktor.ext.getKoin
import org.slf4j.LoggerFactory
import org.valiktor.ConstraintViolationException
import org.valiktor.i18n.toMessage
import java.net.URI
import java.util.*
import java.util.concurrent.TimeUnit


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
        failedVedtaksmeldingProcessor.startAsync(retryOnFail = true)

        Runtime.getRuntime().addShutdownHook(Thread {
            vedtaksmeldingProcessor.stop()
            failedVedtaksmeldingProcessor.stop()
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

    install(StatusPages) {
        val LOGGER = LoggerFactory.getLogger("StatusPages")

        exception<Throwable> { cause ->
            val errorId = UUID.randomUUID()
            LOGGER.error("Uventet feil, $errorId", cause)
            val problem = Problem(
                    title = "Uventet feil",
                    detail = cause.message,
                    instance = URI.create("urn:spion:uventent-feil:$errorId")
            )
            call.respond(HttpStatusCode.InternalServerError, problem)
        }

        exception<MissingKotlinParameterException> { cause ->
            call.respond(
                    HttpStatusCode.BadRequest,
                    ValidationProblem(setOf(
                            ValidationProblemDetail("NotNull", cause.msg, cause.path.joinToString(".") { it.fieldName }, "null"))
                    )
            )
        }

        exception<JsonMappingException> { cause ->
            if (cause.cause is ConstraintViolationException) {
                // Siden valideringen foregår i init {} blokken vil
                // Jackson kunne støte på constrainViolations under de-serialisering.
                // disse vil vi vise til klienten som valideringsfeil
                val constraintViolationException = cause.cause as ConstraintViolationException

                val problems = constraintViolationException.constraintViolations.map {
                    ValidationProblemDetail(it.constraint.name, it.toMessage().message, it.property, it.value)
                }.toSet()

                call.respond(HttpStatusCode.BadRequest, ValidationProblem(problems))
            } else {
                val errorId = UUID.randomUUID()
                LOGGER.warn(errorId.toString(), cause)
                val problem = Problem(
                        title = "Feil ved prosessering av dataene som ble oppgitt",
                        detail = cause.message,
                        instance = URI.create("urn:spion:json-mapping-error:$errorId")
                )
                call.respond(HttpStatusCode.BadRequest, problem)
            }
        }

        exception<ConstraintViolationException> { cause ->
            val problems = cause.constraintViolations.map {
                ValidationProblemDetail(it.constraint.name, it.toMessage().message, it.property, it.value)
            }.toSet()

            call.respond(HttpStatusCode.BadRequest, ValidationProblem(problems))
        }
    }

    nais()

    localCookieDispenser(config)

    routing {
        authenticate {
            spion(get(), get())
        }
    }
}