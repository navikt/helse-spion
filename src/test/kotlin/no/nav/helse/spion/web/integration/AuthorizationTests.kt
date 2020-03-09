package no.nav.helse.spion.web.integration

import com.fasterxml.jackson.databind.ObjectMapper
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import io.ktor.server.testing.setBody
import no.nav.helse.spion.web.dto.OppslagDto
import no.nav.helse.spion.web.spionModule
import org.junit.jupiter.api.Test
import org.koin.core.get
import kotlin.test.assertEquals


class ApplicationAuthorizationTest : ControllerIntegrationTestBase() {

    val noAccessToThisOrg = OppslagDto("20015001543", "123456789")
    val hasAccessToThisOrg = OppslagDto("20015001543", "910020102")

    @Test
    fun `saksOppslag when logged in but unauthorized for the given Virksomhet returns 403 Forbidden`() {
        configuredTestApplication({
            spionModule()
        }) {
            doAuthenticatedRequest(HttpMethod.Post, "/api/v1/ytelsesperioder/oppslag") {
                val objectMapper = get<ObjectMapper>()
                addHeader(HttpHeaders.ContentType, ContentType.Application.Json.toString())
                setBody(objectMapper.writeValueAsString(noAccessToThisOrg))
            }.apply {
                assertEquals(HttpStatusCode.Forbidden, response.status())
            }
        }
    }

    @Test
    fun `saksOppslag when logged in and authorized for the given Virksomhet returns 200 OK`() {
        configuredTestApplication( {
            spionModule()
        }) {
            doAuthenticatedRequest(HttpMethod.Post, "/api/v1/ytelsesperioder/oppslag") {
                val objectMapper = get<ObjectMapper>()
                addHeader(HttpHeaders.ContentType, ContentType.Application.Json.toString())
                setBody(objectMapper.writeValueAsString(hasAccessToThisOrg))
            }.apply {
                assertEquals(HttpStatusCode.OK, response.status())
            }
        }
    }
}