package no.nav.helse.spion.auth.altinn

import io.ktor.client.HttpClient
import io.ktor.client.request.get
import kotlinx.coroutines.runBlocking
import no.nav.helse.spion.auth.AuthorizationsRepository
import no.nav.helse.spion.selfcheck.SelfCheck

class AltinnClient(
        altinnBaseUrl : String,
        private val apiGwApiKey : String,
        private val altinnApiKey : String,
        serviceCode : String,
        private val httpClient: HttpClient) : AuthorizationsRepository, SelfCheck {

    private val baseUrl = "$altinnBaseUrl/reportees?ForceEIAuthentication&serviceCode=$serviceCode&subject="

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

    override fun doSelfCheck() {
        hentRettigheterForPerson("01065500791")
    }
}