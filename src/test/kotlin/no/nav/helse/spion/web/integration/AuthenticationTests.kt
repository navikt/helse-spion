package no.nav.helse.spion.web.integration

import com.fasterxml.jackson.databind.ObjectMapper
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import io.ktor.server.testing.handleRequest
import io.ktor.server.testing.setBody
import no.nav.helse.spion.web.dto.OppslagDto
import no.nav.helse.spion.web.spionModule
import no.nav.security.token.support.test.JwtTokenGenerator
import org.junit.jupiter.api.Test
import org.koin.core.get
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals

class ApplicationAuthenticationTest : ControllerIntegrationTestBase() {

    val oppslag = OppslagDto("200150015432", "987654321", null)

    @Test
    fun saksOppslag_withMissingJWTShouldGive_401_Unauthorized() {
        configuredTestApplication({
            spionModule()
        }) {

            handleRequest(HttpMethod.Post, "/api/v1/saker/oppslag") {

            }.apply {
                assertEquals(HttpStatusCode.Unauthorized, response.status())
            }
        }
    }

    @Test
    fun nais_isalive_endpoint_withMissingJWTShouldGive_200_OK() {
        configuredTestApplication({
            spionModule()
        }) {
            handleRequest(HttpMethod.Get, "/isalive") {
            }.apply {
                assertEquals(HttpStatusCode.OK, response.status())
            }
        }
    }

    @Test
    fun nais_isready_endpoint_withMissingJWTShouldGive_200_OK() {
        configuredTestApplication({
            spionModule()
        }) {
            handleRequest(HttpMethod.Get, "/isready") {
            }.apply {
                assertEquals(HttpStatusCode.OK, response.status())
            }
        }
    }

    @Test
    fun saksOppslag_withValidJWTinHeaderShouldNotGive_401_Unauthorized() {
        configuredTestApplication( {
            spionModule()
        }) {
            handleRequest(HttpMethod.Post, "/api/v1/saker/oppslag") {
                val objectMapper = get<ObjectMapper>()

                addHeader("Authorization", "Bearer ${JwtTokenGenerator.createSignedJWT("header-test").serialize()}")
                addHeader(HttpHeaders.ContentType, ContentType.Application.Json.toString())
                setBody(objectMapper.writeValueAsString(oppslag))
            }.apply {
                assertNotEquals(HttpStatusCode.Unauthorized, response.status())
            }
        }
    }

    @Test
    fun saksOppslag_withValidJWTinCookieShouldNotGive_401() {
        configuredTestApplication({
            spionModule()
        }) {
            doAuthenticatedRequest(HttpMethod.Get, "/api/v1/saker/oppslag") {

            }.apply {
                assertNotEquals(HttpStatusCode.Unauthorized, response.status())
            }
        }
    }

}