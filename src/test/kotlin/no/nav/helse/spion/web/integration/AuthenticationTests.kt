package no.nav.helse.spion.web.integration

import com.fasterxml.jackson.databind.ObjectMapper
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import io.ktor.server.testing.handleRequest
import io.ktor.server.testing.setBody
import io.ktor.util.KtorExperimentalAPI
import no.nav.helse.spion.web.dto.PersonOppslagDto
import no.nav.helse.spion.web.spionModule
import no.nav.helse.validWithoutPeriode
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.koin.core.get

@KtorExperimentalAPI
class ApplicationAuthenticationTest : ControllerIntegrationTestBase() {

    val oppslag = PersonOppslagDto.validWithoutPeriode()

    @Test
    fun `saksOppslag with Missing JWT returns 401 Unauthorized`() {
        configuredTestApplication({
            spionModule()
        }) {

            handleRequest(HttpMethod.Post, "/api/v1/ytelsesperioder/oppslag") {

            }.apply {
                assertThat(response.status()).isEqualTo(HttpStatusCode.Unauthorized)
            }
        }
    }

    @Test
    fun `nais isalive endpoint with no JWT returns 200 OK`() {
        configuredTestApplication({
            spionModule()
        }) {
            handleRequest(HttpMethod.Get, "/isalive") {
            }.apply {
                assertThat(response.status()).isEqualTo(HttpStatusCode.OK)
            }
        }
    }

    @Test
    fun `nais isready endpoint with no JWT returns 200 OK`() {
        configuredTestApplication({
            spionModule()
        }) {
            handleRequest(HttpMethod.Get, "/isready") {
            }.apply {
                assertThat(response.status()).isEqualTo(HttpStatusCode.OK)
            }
        }
    }

    @Test
    fun `saksOppslag with Valid JWT in Header does not return 401 Unauthorized`() {
        configuredTestApplication( {
            spionModule()
        }) {
            handleRequest(HttpMethod.Post, "/api/v1/saker/oppslag") {
                val objectMapper = get<ObjectMapper>()
                addHeader("Authorization", "Bearer ${server?.issueToken("header-test")?.serialize()}")
                addHeader(HttpHeaders.ContentType, ContentType.Application.Json.toString())
                setBody(objectMapper.writeValueAsString(oppslag))
            }.apply {
                assertThat(response.status()).isNotEqualTo(HttpStatusCode.Unauthorized)
            }
        }
    }

    @Test
    fun `saksOppslag with Valid JWT in Cookie does not return 401`() {
        configuredTestApplication({
            spionModule()
        }) {
            doAuthenticatedRequest(HttpMethod.Get, "/api/v1/saker/oppslag") {

            }.apply {
                assertThat(response.status()).isNotEqualTo(HttpStatusCode.Unauthorized)
            }
        }
    }
}