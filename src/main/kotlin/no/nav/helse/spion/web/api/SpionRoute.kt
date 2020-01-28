package no.nav.helse.spion.web.api

import io.ktor.application.ApplicationCallPipeline
import io.ktor.application.application
import io.ktor.application.call
import io.ktor.auth.ForbiddenResponse
import io.ktor.request.receive
import io.ktor.response.respond
import io.ktor.routing.Route
import io.ktor.routing.post
import io.ktor.routing.route
import io.ktor.util.KtorExperimentalAPI
import no.nav.helse.spion.auth.Authorizer
import no.nav.helse.spion.auth.hentIdentitetsnummerFraLoginToken
import no.nav.helse.spion.domenetjenester.SpionService
import no.nav.helse.spion.web.dto.OppslagDto

@KtorExperimentalAPI
fun Route.spion(service: SpionService, authorizer: Authorizer) {
    route("api/v1") {
        route("/saker") {
            intercept(ApplicationCallPipeline.Call) {
                val test = call.receive<OppslagDto>()
                val identitetsnummer = hentIdentitetsnummerFraLoginToken(application.environment.config, call.request)

                if (!authorizer.hasAccess(identitetsnummer, test.arbeidsgiverOrgnr)) {
                    call.respond(ForbiddenResponse())
                    finish()
                }
            }

            post("/oppslag") {
                val oppslag = call.receive<OppslagDto>()
                call.respond(service.hentSakerForPerson(oppslag.identitetsnummer, oppslag.arbeidsgiverOrgnr, oppslag.arbeidsgiverIdentitetsnummer))
            }
        }
        route("/arbeidsgivere") {
            post("/oppslag") {
                call.respond(service.hentArbeidsgivere("TODO"))
            }
        }
    }
}