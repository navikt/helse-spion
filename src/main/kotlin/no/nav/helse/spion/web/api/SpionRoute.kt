package no.nav.helse.spion.web.api

import io.ktor.application.ApplicationCall
import io.ktor.application.application
import io.ktor.application.call
import io.ktor.auth.ForbiddenResponse
import io.ktor.locations.get
import io.ktor.request.receive
import io.ktor.response.respond
import io.ktor.routing.Route
import io.ktor.routing.get
import io.ktor.routing.post
import io.ktor.routing.route
import io.ktor.util.KtorExperimentalAPI
import io.ktor.util.pipeline.PipelineContext
import no.nav.helse.spion.auth.Authorizer
import no.nav.helse.spion.auth.hentIdentitetsnummerFraLoginToken
import no.nav.helse.spion.domenetjenester.SpionService
import no.nav.helse.spion.web.dto.PersonOppslagDto
import no.nav.helse.spion.web.dto.VirksomhetsOppslagDTO

@KtorExperimentalAPI
fun Route.spion(service: SpionService, authorizer: Authorizer) {
    route("api/v1") {
        route("/ytelsesperioder") {

            post("/oppslag") {
                val oppslag = call.receive<PersonOppslagDto>()
                authorize(authorizer, oppslag.arbeidsgiverId)
                call.respond(service.hentYtelserForPerson(oppslag.identitetsnummer, oppslag.arbeidsgiverId, oppslag.periode))
            }

            get<VirksomhetsOppslagDTO> { listing ->
                authorize(authorizer, listing.virksomhetsnummer)
                call.respond(service.hentYtelserForVirksomhet(listing.virksomhetsnummer, listing.periode))
            }
        }

        route("/arbeidsgivere") {
            get("/") {
                val id = hentIdentitetsnummerFraLoginToken(application.environment.config, call.request)
                call.respond(service.hentArbeidsgivere(id))
            }
        }
    }
}

private suspend fun PipelineContext<Unit, ApplicationCall>.authorize(authorizer: Authorizer, arbeidsgiverId: String) {
    val identitetsnummer = hentIdentitetsnummerFraLoginToken(application.environment.config, call.request)
    if (!authorizer.hasAccess(identitetsnummer, arbeidsgiverId)) {
        call.respond(ForbiddenResponse())
        finish()
    }
}