package no.nav.helse.spion.web.api

import io.ktor.application.call
import io.ktor.request.receive
import io.ktor.response.respond
import io.ktor.routing.Route
import io.ktor.routing.post
import io.ktor.routing.route
import no.nav.helse.spion.domenetjenester.SpionService
import no.nav.helse.spion.web.dto.OppslagDto


fun Route.spion(service: SpionService) {
    route("api/v1") {
        route("/saker") {
            post("/oppslag") {
                val oppslag = call.receive<OppslagDto>()
                call.respond(service.hentSakerForPerson(oppslag.identitetsnummer, oppslag.arbeidsgiverOrgnr, oppslag.arbeidsgiverIdentitetsnummer))
            }
        }
    }
}