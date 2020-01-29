package no.nav.helse.spion.web.integration

import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import io.ktor.server.testing.handleRequest
import io.ktor.server.testing.withTestApplication
import no.nav.helse.spion.auth.MockAuthRepo
import no.nav.helse.spion.web.spionModule
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals

class SelfCheckTests : ControllerIntegrationTestBase() {


    @Test
    fun selfCheckEndpointShouldGive500WhenSelfCheckComponentsFail() {
        withTestApplication({
            spionModule(testConfig)
            getKoin().get<MockAuthRepo>().failSelfCheck = true

        }) {
            handleRequest(HttpMethod.Get, "/selfcheck") {
            }.apply {
                assertEquals(HttpStatusCode.InternalServerError, response.status())
            }
        }
    }

    @Test
    fun selfCheckEndpointShouldGive200WhenSelfCheckComponentsAreAllOk() {
        withTestApplication({
            spionModule(testConfig)
            getKoin().get<MockAuthRepo>().failSelfCheck = false

        }) {
            handleRequest(HttpMethod.Get, "/selfcheck") {
            }.apply {
                assertEquals(HttpStatusCode.OK, response.status())
            }
        }
    }


}