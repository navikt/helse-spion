package no.nav.helse.spion.nais

import io.ktor.application.Application
import io.ktor.application.call
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.response.respond
import io.ktor.response.respondText
import io.ktor.response.respondTextWriter
import io.ktor.routing.get
import io.ktor.routing.routing
import io.prometheus.client.CollectorRegistry
import io.prometheus.client.exporter.common.TextFormat
import io.prometheus.client.hotspot.DefaultExports
import no.nav.helse.spion.selfcheck.SelfCheck
import no.nav.helse.spion.selfcheck.SelfCheckState
import no.nav.helse.spion.selfcheck.runSelfChecks
import no.nav.helse.spion.web.getAllOfType
import org.koin.ktor.ext.getKoin
import java.util.*

private val collectorRegistry = CollectorRegistry.defaultRegistry

fun Application.nais(
    isAliveCheck: () -> Boolean = { true },
    isReadyCheck: () -> Boolean = { true }
) {

    DefaultExports.initialize()

    routing {
        get("/isalive") {
            if (!isAliveCheck()) {
                call.respondText("NOT ALIVE", ContentType.Text.Plain, HttpStatusCode.ServiceUnavailable)
            } else {
                call.respondText("ALIVE", ContentType.Text.Plain)
            }
        }

        get("/isready") {
            if (!isReadyCheck()) {
                call.respondText("NOT READY", ContentType.Text.Plain, HttpStatusCode.ServiceUnavailable)
            } else {
                call.respondText("READY", ContentType.Text.Plain)
            }
        }

        get("/metrics") {
            val names = call.request.queryParameters.getAll("name[]")?.toSet() ?: Collections.emptySet()
            call.respondTextWriter(ContentType.parse(TextFormat.CONTENT_TYPE_004)) {
                TextFormat.write004(this, collectorRegistry.filteredMetricFamilySamples(names))
            }
        }

        get("/selfcheck") {
            val allRegisteredSelfCheckComponents = this@routing.getKoin().getAllOfType<SelfCheck>()
            val checkResults = runSelfChecks(allRegisteredSelfCheckComponents)
            val httpResult = if (checkResults.any { it.state == SelfCheckState.ERROR }) HttpStatusCode.InternalServerError else HttpStatusCode.OK

            call.respond(httpResult, checkResults)
        }
    }
}
