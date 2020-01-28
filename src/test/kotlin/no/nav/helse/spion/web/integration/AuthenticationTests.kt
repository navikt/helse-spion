package no.nav.helse.spion.web.integration

import com.fasterxml.jackson.databind.ObjectMapper
import io.ktor.config.MapApplicationConfig
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import io.ktor.server.testing.handleRequest
import io.ktor.server.testing.setBody
import io.ktor.server.testing.withTestApplication
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
        withTestApplication({
            spionModule(testConfig)
        }) {

            handleRequest(HttpMethod.Post, "/api/v1/saker/oppslag") {

            }.apply {
                assertEquals(HttpStatusCode.Unauthorized, response.status())
            }
        }
    }

    @Test
    fun nais_isalive_endpoint_withMissingJWTShouldGive_200_OK() {
        withTestApplication({
            spionModule(testConfig)
        }) {
            handleRequest(HttpMethod.Get, "/isalive") {
            }.apply {
                assertEquals(HttpStatusCode.OK, response.status())
            }
        }
    }

    @Test
    fun nais_isready_endpoint_withMissingJWTShouldGive_200_OK() {
        withTestApplication({
            spionModule(testConfig)
        }) {
            handleRequest(HttpMethod.Get, "/isready") {
            }.apply {
                assertEquals(HttpStatusCode.OK, response.status())
            }
        }
    }

    @Test
    fun saksOppslag_withValidJWTinHeaderShouldNotGive_401_Unauthorized() {
        withTestApplication( {
            addIntegrationTestConfigValues(config = environment.config as MapApplicationConfig)
            spionModule()
        }) {
            handleRequest(HttpMethod.Post, "/api/v1/saker/oppslag") {
                val jwt = JwtTokenGenerator.createSignedJWT("010285295122")
                val objectMapper = get<ObjectMapper>()

                addHeader("Authorization", "Bearer ${jwt.serialize()}")
                addHeader(HttpHeaders.ContentType, ContentType.Application.Json.toString())
                setBody(objectMapper.writeValueAsString(oppslag))
            }.apply {
                assertNotEquals(HttpStatusCode.Unauthorized, response.status())
            }
        }
    }

    @Test
    fun saksOppslag_withValidJWTinCookieShouldNotGive_401() {
        withTestApplication({
            spionModule(testConfig)
        }) {
            handleRequest(HttpMethod.Get, "/api/v1/saker/oppslag") {
                val jwt = JwtTokenGenerator.createSignedJWT("testuser")
                addHeader(HttpHeaders.Cookie, "$idTokenCookieName=${jwt.serialize()}")
            }.apply {
                assertNotEquals(HttpStatusCode.Unauthorized, response.status())
            }
        }
    }

}