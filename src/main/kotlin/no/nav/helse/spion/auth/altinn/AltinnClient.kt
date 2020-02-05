package no.nav.helse.spion.auth.altinn

import io.ktor.client.HttpClient
import io.ktor.client.request.get
import io.ktor.client.response.readText
import io.ktor.http.HttpStatusCode
import kotlinx.coroutines.runBlocking
import no.nav.helse.spion.auth.AuthorizationsRepository
import no.nav.helse.spion.domene.AltinnOrganisasjon
import no.nav.helse.spion.selfcheck.HealthCheck
import no.nav.helse.spion.selfcheck.HealthCheckType
import org.slf4j.LoggerFactory

class AltinnClient(
        altinnBaseUrl : String,
        private val apiGwApiKey : String,
        private val altinnApiKey : String,
        serviceCode : String,
        private val httpClient: HttpClient) : AuthorizationsRepository, HealthCheck {
    override val healthCheckType = HealthCheckType.READYNESS

    private val baseUrl = "$altinnBaseUrl/reportees/?ForceEIAuthentication&serviceEdition=1&serviceCode=$serviceCode&subject="

    /**
     * @return en liste over organisasjoner og/eller personer den angitte personen har rettigheten for
     */
    override fun hentOrgMedRettigheterForPerson(identitetsnummer: String): Set<AltinnOrganisasjon> {
        log.info(baseUrl)
        log.info(apiGwApiKey.substring(0, 3))
        log.info(altinnApiKey.substring(0, 3))
        val url = baseUrl + identitetsnummer
        return runBlocking {
            httpClient.get<Set<AltinnOrganisasjon>>(url) {
                headers.append("X-NAV-APIKEY", apiGwApiKey)
                headers.append("APIKEY", altinnApiKey)
            }
        }
    }

    override suspend fun doHealthCheck() {
        try {
            hentOrgMedRettigheterForPerson("01065500791")
        } catch (ex: io.ktor.client.features.ClientRequestException) {
            if (!(ex.response.status == HttpStatusCode.BadRequest && ex.response.readText().contains("Invalid social security number"))) {
                throw ex
            }
        }
    }


    companion object {
        private val log = LoggerFactory.getLogger(AltinnClient::class.java)
    }
}