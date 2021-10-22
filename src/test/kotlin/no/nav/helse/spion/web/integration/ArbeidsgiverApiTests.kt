package no.nav.helse.spion.web.integration

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import io.ktor.server.testing.handleRequest
import io.ktor.util.KtorExperimentalAPI
import no.nav.helse.spion.domene.AltinnOrganisasjon
import no.nav.helse.spion.web.spionModule
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Test
import org.koin.core.get
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals

@KtorExperimentalAPI
class ArbeidsgiverApiTests : ControllerIntegrationTestBase() {

    @Test
    fun `should Give 401 When Not Logged In`() {
        configuredTestApplication({
            spionModule()
        }) {
            handleRequest(HttpMethod.Get, "/api/v1/arbeidsgivere") {
            }.apply {
                assertEquals(HttpStatusCode.Unauthorized, response.status())
            }
        }
    }

    @Test
    fun `should Return List Of Organisations When Logged In`() {
        configuredTestApplication({
            spionModule()
        }) {
            doAuthenticatedRequest(HttpMethod.Get, "/api/v1/arbeidsgivere") {
            }.apply {
                val mapper = get<ObjectMapper>()
                assertNotNull(response.content)
                val responseObject = mapper.readValue<Set<AltinnOrganisasjon>>(response.content ?: "[]")
                assertEquals(HttpStatusCode.OK, response.status())
                assertNotEquals(0, responseObject.size)
            }
        }
    }
}
