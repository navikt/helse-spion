package no.nav.helse.spion.nais

import com.fasterxml.jackson.databind.ObjectMapper
import io.ktor.application.Application
import io.ktor.application.ApplicationCall
import io.ktor.application.call
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.response.respond
import io.ktor.response.respondTextWriter
import io.ktor.routing.Routing
import io.ktor.routing.get
import io.ktor.routing.routing
import io.ktor.util.KtorExperimentalAPI
import io.ktor.util.pipeline.PipelineContext
import io.prometheus.client.CollectorRegistry
import io.prometheus.client.exporter.common.TextFormat
import io.prometheus.client.hotspot.DefaultExports
import no.nav.helse.spion.selfcheck.HealthCheck
import no.nav.helse.spion.selfcheck.HealthCheckState
import no.nav.helse.spion.selfcheck.HealthCheckType
import no.nav.helse.spion.selfcheck.runHealthChecks
import no.nav.helse.spion.varsling.mottak.ManglendeInntektsMeldingMelding
import no.nav.helse.spion.vedtaksmelding.PersonGenerator
import no.nav.helse.spion.web.getAllOfType
import no.nav.helse.spion.web.getString
import org.apache.commons.lang.exception.ExceptionUtils
import org.apache.kafka.clients.producer.KafkaProducer
import org.apache.kafka.clients.producer.ProducerRecord
import org.apache.kafka.common.serialization.StringSerializer
import org.koin.ktor.ext.get
import org.koin.ktor.ext.getKoin
import org.slf4j.LoggerFactory
import java.time.LocalDate
import java.util.*
import java.util.concurrent.TimeUnit

private val collectorRegistry = CollectorRegistry.defaultRegistry

@KtorExperimentalAPI
fun Application.nais() {

    DefaultExports.initialize()
    val pg = PersonGenerator(4)

    routing {
        get("/isalive") {
            returnResultOfChecks(this@routing, HealthCheckType.ALIVENESS, this)
        }

        get("/isready") {
            returnResultOfChecks(this@routing, HealthCheckType.READYNESS, this)
        }

        get("/metrics") {
            val names = call.request.queryParameters.getAll("name[]")?.toSet() ?: Collections.emptySet()
            call.respondTextWriter(ContentType.parse(TextFormat.CONTENT_TYPE_004)) {
                TextFormat.write004(this, collectorRegistry.filteredMetricFamilySamples(names))
            }
        }

        get("/healthcheck") {
            val allRegisteredSelfCheckComponents = this@routing.getKoin().getAllOfType<HealthCheck>()
            val checkResults = runHealthChecks(allRegisteredSelfCheckComponents)
            val httpResult = if (checkResults.any { it.state == HealthCheckState.ERROR }) HttpStatusCode.InternalServerError else HttpStatusCode.OK

            call.respond(httpResult, checkResults)
        }

        get("/send-altinn-melding") {

            val log = LoggerFactory.getLogger("/send-altinn-melding")

            if (environment.config.property("koin.profile").getString() == "PROD") {
                call.respond(HttpStatusCode.ExpectationFailed, "Kan ikke kalles i PROD")
                return@get
            }

            try {
                val cfg = this@routing.application.environment.config

                val topicName = cfg.getString("altinn_melding.kafka_topic")
                val producer = KafkaProducer<String, String>(mutableMapOf<String, Any>(
                            "bootstrap.servers" to cfg.getString("kafka.endpoint")
                ), StringSerializer(), StringSerializer())

                val om = this@routing.get<ObjectMapper>()
                val person = pg.getRandomPerson()

                val messageString = om.writeValueAsString(ManglendeInntektsMeldingMelding(
                        "810007842", //  -> Anstendig Piggsvin Barnehage
                        LocalDate.now().minusDays(1),
                        LocalDate.now().plusDays(7),
                        person.identitetsnummer,
                        person.fornavn + " " + person.etternavn
                ))
                producer.send(ProducerRecord(topicName, messageString)).get(10, TimeUnit.SECONDS)

                call.respond(HttpStatusCode.OK, "Melding sendt til Kø: \n$messageString")
            } catch (t: Throwable) {
                call.respond(HttpStatusCode.InternalServerError, t.message + "\n\n" + ExceptionUtils.getStackTrace(t))
            }
        }
    }
}

private suspend fun returnResultOfChecks(routing: Routing, type: HealthCheckType, pipelineContext: PipelineContext<Unit, ApplicationCall>) {
    val allRegisteredSelfCheckComponents = routing.getKoin()
            .getAllOfType<HealthCheck>()
            .filter { it.healthCheckType == type }

    val checkResults = runHealthChecks(allRegisteredSelfCheckComponents)
    val httpResult = if (checkResults.any { it.state == HealthCheckState.ERROR }) HttpStatusCode.InternalServerError else HttpStatusCode.OK
    checkResults.forEach { r ->
        r.error?.let { pipelineContext.call.application.environment.log.error(r.toString()) }
    }
    pipelineContext.call.respond(httpResult, checkResults)
}


