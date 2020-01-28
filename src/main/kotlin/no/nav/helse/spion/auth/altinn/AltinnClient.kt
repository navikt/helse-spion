package no.nav.helse.spion.auth.altinn

import io.ktor.client.HttpClient
import io.ktor.client.engine.apache.Apache
import io.ktor.client.request.get
import kotlinx.coroutines.runBlocking
import no.nav.helse.spion.auth.AuthorizationsRepository

class AltinnClient(
        val altinnBaseUrl : String,
        val apiGwApiKey : String,
        val altinnApiKey : String,
        val serviceCode : String,
        val httpClient: HttpClient = HttpClient(Apache)) : AuthorizationsRepository {

    /**
     * @return en liste over organisasjonsnummer og/eller identitetsnummere den angitte personen har rettigheten for
     */
    override fun hentRettigheterForPerson(identitetsnummer: String): Set<String> {
        val url = "$altinnBaseUrl/reportees?ForceEIAuthentication&subject=$identitetsnummer&serviceCode=$serviceCode"
        val res = runBlocking {
            httpClient.get<List<AltinnOrganisasjon>>(url) {
                headers.append("X-NAV-APIKEY", apiGwApiKey)
                headers.append("APIKEY", altinnApiKey)
            }
        }

        return res.mapNotNull { it.organizationNumber ?: it.socialSecurityNumber }.toSet()
    }
}