package no.nav.helse.spion.auth.altinn

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

    val validAltinnResponse = "[{\"Name\":\"ELIAS MYRDAL\",\"Type\":\"Person\",\"SocialSecurityNumber\":\"01065500791\"},{\"Name\":\"HØNEFOSS OG ØLEN\",\"Type\":\"Enterprise\",\"OrganizationNumber\":\"910020102\",\"OrganizationForm\":\"AS\",\"Status\":\"Active\"},{\"Name\":\"JØA OG SEL\",\"Type\":\"Business\",\"OrganizationNumber\":\"910098896\",\"ParentOrganizationNumber\":\"910026194\",\"OrganizationForm\":\"BEDR\",\"Status\":\"Active\"},{\"Name\":\"RØST OG VOSS\",\"Type\":\"Business\",\"OrganizationNumber\":\"910559451\",\"OrganizationForm\":\"BEDR\",\"Status\":\"Active\"},{\"Name\":\"STADLANDET OG SINGSÅS\",\"Type\":\"Enterprise\",\"OrganizationNumber\":\"911366940\",\"OrganizationForm\":\"AS\",\"Status\":\"Active\"}]"
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
    internal fun valid_answer_from_altinn_returns_properly_serialized_list_of_all_org_forms() {
        val altinnClient = AltinnClient("http://juice", "api-gw-key", "altinn-key", serviceCode, client)
        val authList = altinnClient.hentRettigheterForPerson(identitetsnummer)
        assert(authList.size == 5)
        assertThat(authList).contains("01065500791") // skal inneholde identitetsnummer i tilfeller der rettighet er gitt på person
    }
    @Test
    internal fun timeout_from_altinn_throws() {
        val altinnClient = AltinnClient("http://timeout", "api-gw-key", "altinn-key", serviceCode, client)

        assertThrows(ServerResponseException::class.java) {
            altinnClient.hentRettigheterForPerson(identitetsnummer)
        }
    }
}