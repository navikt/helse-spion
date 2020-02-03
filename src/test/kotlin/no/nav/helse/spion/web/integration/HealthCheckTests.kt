package no.nav.helse.spion.web.integration

import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import io.ktor.server.testing.handleRequest
import io.ktor.server.testing.withTestApplication
import io.ktor.util.KtorExperimentalAPI
import no.nav.helse.spion.auth.MockAuthRepo
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
            get<MockAuthRepo>().failSelfCheck = true

        }) {
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
            get<MockAuthRepo>().failSelfCheck = false

        }) {
            handleRequest(HttpMethod.Get, "/healthcheck") {
            }.apply {
                assertEquals(HttpStatusCode.OK, response.status())
            }
        }
    }


}