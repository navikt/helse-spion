package no.nav.helse.spion.web.api

import io.ktor.application.call
import io.ktor.response.respond
import io.ktor.routing.Route
import io.ktor.routing.get
import no.nav.helse.spion.domenetjenester.SpionService


fun Route.spion(service: SpionService) {
    get("api/spion") {
        call.respond(service.hentSaksinformasjon())
    }
}