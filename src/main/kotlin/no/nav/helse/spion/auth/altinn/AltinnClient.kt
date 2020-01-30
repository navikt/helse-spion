package no.nav.helse.spion.auth.altinn

import io.ktor.client.HttpClient
import io.ktor.client.request.get
import io.ktor.client.response.readText
import io.ktor.http.HttpStatusCode
import kotlinx.coroutines.runBlocking
import no.nav.helse.spion.auth.AuthorizationsRepository
import no.nav.helse.spion.selfcheck.HealthCheck
import no.nav.helse.spion.selfcheck.HealthCheckType

class AltinnClient(
        altinnBaseUrl : String,
        private val apiGwApiKey : String,
        private val altinnApiKey : String,
        serviceCode : String,
        private val httpClient: HttpClient) : AuthorizationsRepository, HealthCheck {
    override val healthCheckType = HealthCheckType.READYNESS

    private val baseUrl = "$altinnBaseUrl/reportees?ForceEIAuthentication&serviceEdition=1&serviceCode=$serviceCode&subject="

    /**
     * @return en liste over organisasjonsnummer og/eller identitetsnummere den angitte personen har rettigheten for
     */
    override fun hentRettigheterForPerson(identitetsnummer: String): Set<String> {
        val url = baseUrl + identitetsnummer
        val res = runBlocking {
            httpClient.get<List<AltinnOrganisasjon>>(url) {
                headers.append("X-NAV-APIKEY", apiGwApiKey)
                headers.append("APIKEY", altinnApiKey)
            }
        }

        return res.mapNotNull { it.organizationNumber ?: it.socialSecurityNumber }.toSet()
    }

    override suspend fun doHealthCheck() {
        try {
            hentRettigheterForPerson("01065500791")
        } catch(ex : io.ktor.client.features.ClientRequestException) {
            if (!(ex.response.status == HttpStatusCode.BadRequest && ex.response.readText().contains("Invalid social security number"))) {
                throw ex
            }
        }
    }
}