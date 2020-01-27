package no.nav.helse.spion.web.integration

import com.github.tomakehurst.wiremock.WireMockServer
import com.github.tomakehurst.wiremock.client.WireMock
import com.github.tomakehurst.wiremock.core.WireMockConfiguration
import io.ktor.config.ApplicationConfig
import io.ktor.config.MapApplicationConfig
import io.ktor.util.KtorExperimentalAPI
import no.nav.security.token.support.test.JwkGenerator
import no.nav.security.token.support.test.JwtTokenGenerator
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.BeforeAll
import org.koin.test.KoinTest

@KtorExperimentalAPI
open class ControllerIntegrationTestBase : KoinTest {

    val testConfig : ApplicationConfig
    protected val idTokenCookieName = "selvbetjening-idtoken"

    init {
        testConfig = MapApplicationConfig()
        addIntegrationTestConfigValues(testConfig)
    }

    @KtorExperimentalAPI
    fun addIntegrationTestConfigValues(config : MapApplicationConfig, acceptedIssuer:String = JwtTokenGenerator.ISS, acceptedAudience:String = JwtTokenGenerator.AUD) {
        config.apply {
            put("koin.profile", "LOCAL")
            put("no.nav.security.jwt.issuers.size", "1")
            put("no.nav.security.jwt.issuers.0.issuer_name", acceptedIssuer)
            put("no.nav.security.jwt.issuers.0.discoveryurl", server.baseUrl() + "/.well-known/openid-configuration")
            put("no.nav.security.jwt.issuers.0.accepted_audience", acceptedAudience)
            put("no.nav.security.jwt.issuers.0.cookie_name", idTokenCookieName)
        }
    }

    companion object {
        val server: WireMockServer = WireMockServer(WireMockConfiguration.options().dynamicPort())
        @BeforeAll
        @JvmStatic
        fun before() {
            server.start()
            WireMock.configureFor(server.port())
            stubOIDCProvider()
        }
        @AfterAll
        @JvmStatic
        fun after() {
            server.stop()
        }

        fun stubOIDCProvider() {
            WireMock.stubFor(WireMock.any(WireMock.urlPathEqualTo("/.well-known/openid-configuration")).willReturn(
                    WireMock.okJson("{\"jwks_uri\": \"${server.baseUrl()}/keys\", " +
                            "\"subject_types_supported\": [\"pairwise\"], " +
                            "\"issuer\": \"${JwtTokenGenerator.ISS}\"}")))

            WireMock.stubFor(WireMock.any(WireMock.urlPathEqualTo("/keys")).willReturn(
                    WireMock.okJson(JwkGenerator.getJWKSet().toPublicJWKSet().toString())))
        }
    }
}