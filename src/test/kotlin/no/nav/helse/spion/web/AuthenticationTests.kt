package no.nav.helse.spion.web

import com.github.tomakehurst.wiremock.WireMockServer
import com.github.tomakehurst.wiremock.client.WireMock.*
import com.github.tomakehurst.wiremock.core.WireMockConfiguration
import io.ktor.application.Application
import io.ktor.config.MapApplicationConfig
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import io.ktor.server.testing.handleRequest
import io.ktor.server.testing.withTestApplication
import io.ktor.util.KtorExperimentalAPI
import no.nav.security.token.support.test.JwkGenerator
import no.nav.security.token.support.test.JwtTokenGenerator
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals

class ApplicationAuthenticationTest {
    companion object {
        val server: WireMockServer = WireMockServer(WireMockConfiguration.options().dynamicPort())
        @BeforeAll
        @JvmStatic
        fun before() {
            server.start()
            configureFor(server.port())
        }
        @AfterAll
        @JvmStatic
        fun after() {
            server.stop()
        }
    }

    private val idTokenCookieName = "selvbetjening-idtoken"

    @Test
    fun saksOppslag_withMissingJWTShouldGive_401_Unauthorized() {
        withTestApplication({
            stubOIDCProvider()
            addMockLoginServiceConfig()
            spionModule(environment.config)
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
            stubOIDCProvider()
            addMockLoginServiceConfig()
            spionModule(environment.config)
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
            stubOIDCProvider()
            addMockLoginServiceConfig()
            spionModule(environment.config)
        }) {
            handleRequest(HttpMethod.Get, "/isready") {
            }.apply {
                assertEquals(HttpStatusCode.OK, response.status())
            }
        }
    }

    @Test
    fun saksOppslag_withValidJWTinHeaderShouldNotGive_401_Unauthorized() {
        withTestApplication({
            stubOIDCProvider()
            addMockLoginServiceConfig()
            spionModule(environment.config)
        }) {
            handleRequest(HttpMethod.Post, "/api/v1/saker/oppslag") {
                val jwt = JwtTokenGenerator.createSignedJWT("testuser")
                addHeader("Authorization", "Bearer ${jwt.serialize()}")
            }.apply {
                assertNotEquals(HttpStatusCode.Unauthorized, response.status())
            }
        }
    }

    @Test
    fun saksOppslag_withValidJWTinCookieShouldNotGive_401() {
        withTestApplication({
            stubOIDCProvider()
            addMockLoginServiceConfig()
            spionModule(environment.config)

        }) {
            handleRequest(HttpMethod.Get, "/api/v1/saker/oppslag") {
                val jwt = JwtTokenGenerator.createSignedJWT("testuser")
                addHeader(HttpHeaders.Cookie, "$idTokenCookieName=${jwt.serialize()}")
            }.apply {
                assertNotEquals(HttpStatusCode.Unauthorized, response.status())
            }
        }
    }

    fun stubOIDCProvider() {
        stubFor(any(urlPathEqualTo("/.well-known/openid-configuration")).willReturn(
                okJson("{\"jwks_uri\": \"${server.baseUrl()}/keys\", " +
                        "\"subject_types_supported\": [\"pairwise\"], " +
                        "\"issuer\": \"${JwtTokenGenerator.ISS}\"}")))

        stubFor(any(urlPathEqualTo("/keys")).willReturn(
                okJson(JwkGenerator.getJWKSet().toPublicJWKSet().toString())))
    }

    @KtorExperimentalAPI
    fun Application.addMockLoginServiceConfig(acceptedIssuer:String = JwtTokenGenerator.ISS, acceptedAudience:String = JwtTokenGenerator.AUD) {
        (environment.config as MapApplicationConfig).apply {
            put("no.nav.security.jwt.issuers.size", "1")
            put("no.nav.security.jwt.issuers.0.issuer_name", acceptedIssuer)
            put("no.nav.security.jwt.issuers.0.discoveryurl", server.baseUrl() + "/.well-known/openid-configuration")
            put("no.nav.security.jwt.issuers.0.accepted_audience", acceptedAudience)
            put("no.nav.security.jwt.issuers.0.cookie_name", idTokenCookieName)
        }
    }
}