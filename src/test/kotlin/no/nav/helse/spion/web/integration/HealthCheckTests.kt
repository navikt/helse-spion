package no.nav.helse.spion.web.integration

import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import io.ktor.server.testing.handleRequest
import io.ktor.util.KtorExperimentalAPI
import no.nav.helse.arbeidsgiver.kubernetes.KubernetesProbeManager
import no.nav.helse.spion.auth.StaticMockAuthRepo
import no.nav.helse.spion.web.spionModule
import org.junit.jupiter.api.Test
import org.koin.ktor.ext.get
import kotlin.test.assertEquals

@KtorExperimentalAPI
class HealthCheckTests : ControllerIntegrationTestBase() {
    @Test
    fun `HealthCheck Endpoint returns 500 When any HealthCheck Component Fail`() {
        configuredTestApplication({
            spionModule()
            get<StaticMockAuthRepo>().failSelfCheck = true
        }) {
            val probeManager = application.get<KubernetesProbeManager>()
            probeManager.registerReadynessComponent(application.get<StaticMockAuthRepo>())
            handleRequest(HttpMethod.Get, "/healthcheck") {
            }.apply {
                assertEquals(HttpStatusCode.InternalServerError, response.status())
            }
        }
    }

    @Test
    fun `HealthCheck Endpoint returns 200 When all HealthCheck Components are Ok`() {
        configuredTestApplication({
            spionModule()
            get<StaticMockAuthRepo>().failSelfCheck = false
        }) {
            val probeManager = application.get<KubernetesProbeManager>()
            probeManager.registerReadynessComponent(application.get<StaticMockAuthRepo>())
            handleRequest(HttpMethod.Get, "/healthcheck") {
            }.apply {
                assertEquals(HttpStatusCode.OK, response.status())
            }
        }
    }
}
