package no.nav.helse.spion.web

import com.fasterxml.jackson.databind.JsonMappingException
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.MissingKotlinParameterException
import io.ktor.application.Application
import io.ktor.application.ApplicationCall
import io.ktor.application.call
import io.ktor.application.install
import io.ktor.auth.Authentication
import io.ktor.auth.authenticate
import io.ktor.config.ApplicationConfig
import io.ktor.features.CORS
import io.ktor.features.ContentNegotiation
import io.ktor.features.DataConversion
import io.ktor.features.ParameterConversionException
import io.ktor.features.StatusPages
import io.ktor.http.ContentType
import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import io.ktor.jackson.JacksonConverter
import io.ktor.locations.Locations
import io.ktor.response.respond
import io.ktor.routing.IgnoreTrailingSlash
import io.ktor.routing.route
import io.ktor.routing.routing
import io.ktor.util.DataConversionException
import no.nav.helse.arbeidsgiver.system.AppEnv
import no.nav.helse.arbeidsgiver.system.getEnvironment
import no.nav.helse.spion.nais.nais
import no.nav.helse.spion.web.api.spion
import no.nav.helse.spion.web.api.systemRoutes
import no.nav.helse.spion.web.dto.validation.Problem
import no.nav.helse.spion.web.dto.validation.ValidationProblem
import no.nav.helse.spion.web.dto.validation.ValidationProblemDetail
import no.nav.security.token.support.ktor.tokenValidationSupport
import org.koin.ktor.ext.get
import org.slf4j.LoggerFactory
import org.valiktor.ConstraintViolationException
import org.valiktor.i18n.toMessage
import java.lang.reflect.InvocationTargetException
import java.net.URI
import java.time.LocalDate
import java.util.UUID
import javax.ws.rs.ForbiddenException

fun Application.spionModule(config: ApplicationConfig = environment.config) {

    install(IgnoreTrailingSlash)
    install(Authentication) {
        tokenValidationSupport(config = config)
    }

    install(Locations)

    configureCORSAccess(config)

    install(ContentNegotiation) {
        val commonObjectMapper = get<ObjectMapper>()
        register(ContentType.Application.Json, JacksonConverter(commonObjectMapper))
    }

    install(DataConversion) {
        convert<LocalDate> {
            // this: DelegatingConversionService
            decode { values, _ ->
                // converter: (values: List<String>, type: Type) -> Any?
                values.singleOrNull()?.let { LocalDate.parse(it) }
            }

            encode { value ->
                // converter: (value: Any?) -> List<String>
                when (value) {
                    null -> listOf()
                    is LocalDate -> listOf(value.toString())
                    else -> throw DataConversionException("Cannot convert $value as LocalDate")
                }
            }
        }
    }

    install(StatusPages) {
        val LOGGER = LoggerFactory.getLogger("StatusPages")

        suspend fun handleUnexpectedException(call: ApplicationCall, cause: Throwable) {
            val errorId = UUID.randomUUID()
            LOGGER.error("Uventet feil, $errorId", cause)
            val problem = Problem(
                type = URI.create("urn:spion:uventet-feil"),
                title = "Uventet feil",
                detail = cause.message,
                instance = URI.create("urn:spion:uventent-feil:$errorId")
            )
            call.respond(HttpStatusCode.InternalServerError, problem)
        }

        suspend fun handleValidationError(call: ApplicationCall, cause: ConstraintViolationException) {
            val problems = cause.constraintViolations.map {
                ValidationProblemDetail(it.constraint.name, it.toMessage().message, it.property, it.value)
            }.toSet()

            call.respond(HttpStatusCode.UnprocessableEntity, ValidationProblem(problems))
        }

        exception<InvocationTargetException> { cause ->
            when (cause.targetException) {
                is ConstraintViolationException -> handleValidationError(call, cause.targetException as ConstraintViolationException)
                else -> handleUnexpectedException(call, cause)
            }
        }

        exception<ForbiddenException> { cause ->
            call.respond(
                HttpStatusCode.Forbidden,
                Problem(URI.create("urn:spion:forbidden"), "Ingen tilgang", HttpStatusCode.Forbidden.value)
            )
        }

        exception<Throwable> { cause ->
            handleUnexpectedException(call, cause)
        }

        exception<ParameterConversionException> { cause ->
            call.respond(
                HttpStatusCode.BadRequest,
                ValidationProblem(
                    setOf(
                        ValidationProblemDetail("ParameterConversion", "Paramteret kunne konverteres til ${cause.type}", cause.parameterName, null)
                    )
                )
            )
        }

        exception<MissingKotlinParameterException> { cause ->
            call.respond(
                HttpStatusCode.BadRequest,
                ValidationProblem(
                    setOf(
                        ValidationProblemDetail("NotNull", cause.message ?: "uknown", cause.path.joinToString(".") { it.fieldName }, "null")
                    )
                )
            )
        }

        exception<JsonMappingException> { cause ->
            // Siden valideringen foregår i init {} blokken vil
            // Jackson kunne støte på constrainViolations under de-serialisering.
            // disse vil vi vise til klienten som valideringsfeil

            when (cause.cause) {
                is ConstraintViolationException -> handleValidationError(call, cause.cause as ConstraintViolationException)
                else -> {
                    val errorId = UUID.randomUUID()
                    LOGGER.warn(errorId.toString(), cause)
                    val problem = Problem(
                        title = "Feil ved prosessering av JSON-dataene som ble oppgitt",
                        detail = cause.message,
                        instance = URI.create("urn:spion:json-mapping-error:$errorId")
                    )
                    call.respond(HttpStatusCode.BadRequest, problem)
                }
            }
        }

        exception<ConstraintViolationException> { cause ->
            handleValidationError(call, cause)
        }
    }

    nais()

    routing {
        // TODO: legg til basepath
        //  val apiBasePath = config.getString("ktor.application.basepath")
        route("/api/v1") {
            authenticate {
                systemRoutes()
                spion(get(), get())
            }
        }
    }
}

private fun Application.configureCORSAccess(config: ApplicationConfig) {
    install(CORS) {
        method(HttpMethod.Options)
        method(HttpMethod.Post)
        method(HttpMethod.Get)

        when (config.getEnvironment()) {
            AppEnv.PROD -> host("arbeidsgiver.nav.no", schemes = listOf("https"))
            else -> anyHost()
        }

        allowCredentials = true
        allowNonSimpleContentTypes = true
    }
}
