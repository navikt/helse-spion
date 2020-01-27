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
import no.nav.helse.spion.auth.AuthorizationsRepository
import no.nav.helse.spion.auth.MockAuthRepo
import no.nav.helse.spion.web.dto.OppslagDto
import no.nav.helse.spion.web.spionModule
import no.nav.security.token.support.test.JwtTokenGenerator
import org.junit.jupiter.api.Test
import org.koin.core.get
import org.mockito.Mock
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals


class ApplicationAuthorizationTest : ControllerIntegrationTestBase() {

    val noAccessToThisOrg = OppslagDto("200150015432", "123456789", null)
    val hasAccessToThisOrg  = OppslagDto("200150015432", "987654321", null)

    @Test
    fun saksOppslag_loggedInButNoAccess_gives_403_forbidden() {
        withTestApplication( {
            addIntegrationTestConfigValues(config = environment.config as MapApplicationConfig)
            spionModule()
        }) {
            handleRequest(HttpMethod.Post, "/api/v1/saker/oppslag") {
                val jwt = JwtTokenGenerator.createSignedJWT("010285295122")
                val objectMapper = get<ObjectMapper>()

                addHeader("Authorization", "Bearer ${jwt.serialize()}")
                addHeader(HttpHeaders.ContentType, ContentType.Application.Json.toString())
                setBody(objectMapper.writeValueAsString(noAccessToThisOrg))
            }.apply {
                assertEquals(HttpStatusCode.Forbidden, response.status())
            }
        }
    }

    @Test
    fun saksOppslag_loggedIn_and_has_access_gives_200_OK() {
        withTestApplication( {
            addIntegrationTestConfigValues(config = environment.config as MapApplicationConfig)
            spionModule()
        }) {
            handleRequest(HttpMethod.Post, "/api/v1/saker/oppslag") {
                val jwt = JwtTokenGenerator.createSignedJWT("010285295122")
                val objectMapper = get<ObjectMapper>()
                val mockAcl = get<AuthorizationsRepository>() as MockAuthRepo
                mockAcl.setAccessList(setOf(hasAccessToThisOrg.arbeidsgiverOrgnr))
                addHeader("Authorization", "Bearer ${jwt.serialize()}")
                addHeader(HttpHeaders.ContentType, ContentType.Application.Json.toString())
                setBody(objectMapper.writeValueAsString(hasAccessToThisOrg))
            }.apply {
                assertEquals(HttpStatusCode.OK, response.status())
            }
        }
    }
}