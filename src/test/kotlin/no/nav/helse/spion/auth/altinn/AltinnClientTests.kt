package no.nav.helse.spion.auth.altinn

import loadFromResources
import com.fasterxml.jackson.databind.MapperFeature
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.features.ServerResponseException
import io.ktor.client.features.json.JacksonSerializer
import io.ktor.client.features.json.JsonFeature
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test

class AltinnClientTests {

    val validAltinnResponse = "mock-data/altinn/organisasjoner-med-rettighet.json".loadFromResources()

    private val identitetsnummer = "01020354321"
    private val serviceCode = "4444"

    val client = HttpClient(MockEngine) {

        install(JsonFeature) { serializer = JacksonSerializer {
            configure(MapperFeature.ACCEPT_CASE_INSENSITIVE_PROPERTIES, true)
        } }

        engine {
            addHandler { request ->
                when (request.url.toString()) {
                    "http://juice/reportees?ForceEIAuthentication&serviceEdition=1&serviceCode=$serviceCode&subject=$identitetsnummer" -> {
                        val responseHeaders = headersOf("Content-Type" to listOf(ContentType.Application.Json.toString()))
                        respond(validAltinnResponse, headers = responseHeaders)
                    }
                    "http://timeout/reportees?ForceEIAuthentication&serviceEdition=1&serviceCode=$serviceCode&subject=$identitetsnummer" -> {
                        respond("Timed out", HttpStatusCode.GatewayTimeout)
                    }
                    else -> error("Unhandled ${request.url}")
                }
            }
        }
    }

    @Test
    internal fun `valid answer from altinn returns properly serialized list of all org forms`() {
        val altinnClient = AltinnClient("http://juice", "api-gw-key", "altinn-key", serviceCode, client)
        val authList = altinnClient.hentOrgMedRettigheterForPerson(identitetsnummer)
        assert(authList.size == 5)
        assertThat(authList.find { it.socialSecurityNumber ==  "01065500791"}).isNotNull
    }
    @Test
    internal fun `timeout from altinn throws exception`() {
        val altinnClient = AltinnClient("http://timeout", "api-gw-key", "altinn-key", serviceCode, client)

        assertThrows(ServerResponseException::class.java) {
            altinnClient.hentOrgMedRettigheterForPerson(identitetsnummer)
        }
    }
}