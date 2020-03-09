package no.nav.helse.spion.web.dto.validation

import io.ktor.application.ApplicationCallPipeline
import io.ktor.application.call
import io.ktor.request.receive
import io.ktor.routing.Route
import java.net.URI


interface Validable {
    fun validate()
}

inline fun <reified T : Any> Route.validatePayload() {
    intercept(ApplicationCallPipeline.Call) {
        val data: T = this.call.receive()
        (data as Validable).let {
            it.validate()
        }
    }
}

/**
 * https://tools.ietf.org/html/rfc7807#page-5
 */
open class Problem(
        val type: URI = URI.create("about:blank"),
        val title: String,
        val status: Int? = 500,
        val detail: String? = null,
        val instance: URI = URI.create("about:blank")
)

/**
 * Problem extension for input-validation-feil.
 * Inneholder en liste over properties som feilet validering
 */
class ValidationProblem(
        val violations: Set<ValidationProblemDetail>
) : Problem(
        URI.create("urn:spion:validation-error"),
        "Valideringen av input feilet",
        400,
        "Se 'violations' for detaljer"
)

class ValidationProblemDetail(
        val validationType: String, val message: String, val propertyPath: String, val invalidValue: Any?)
