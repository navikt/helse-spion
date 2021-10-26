package no.nav.helse.spion.web.integration

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import io.ktor.util.KtorExperimentalAPI
import no.nav.helse.TestData
import no.nav.helse.spion.domene.ytelsesperiode.Ytelsesperiode
import no.nav.helse.spion.web.spionModule
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Test
import org.koin.core.component.get
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals

@KtorExperimentalAPI
class VirksomhetsOppslagTests : ControllerIntegrationTestBase() {
    private val forbiddenUrl = "/api/v1/ytelsesperioder/virksomhet/${TestData.validOrgNr}?fom=2010-01-01&tom=2100-01-01"
    private val validUrl = "/api/v1/ytelsesperioder/virksomhet/917404437?fom=2010-01-01&tom=2100-01-01"

    @Test
    fun `403 dersom virksomheten ikke er i tilgangslisten`() {
        configuredTestApplication({
            spionModule()
        }) {
            doAuthenticatedRequest(HttpMethod.Get, validUrl) {
            }.apply {
                val mapper = get<ObjectMapper>()
                assertNotNull(response.content)
                val responseObject = mapper.readValue<List<Ytelsesperiode>>(response.content ?: "[]")
                assertEquals(HttpStatusCode.OK, response.status())
                assertNotEquals(0, responseObject.size)
            }
        }
    }

    @Test
    fun `Returnerer Ytelsesperioder når URLen er gyldig`() {
        configuredTestApplication({
            spionModule()
        }) {
            doAuthenticatedRequest(HttpMethod.Get, validUrl) {
            }.apply {
                val mapper = get<ObjectMapper>()
                assertNotNull(response.content)
                val responseObject = mapper.readValue<List<Ytelsesperiode>>(response.content ?: "[]")
                assertEquals(HttpStatusCode.OK, response.status())
                assertNotEquals(0, responseObject.size)
            }
        }
    }
}
