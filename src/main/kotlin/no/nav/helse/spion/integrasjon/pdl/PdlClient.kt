package no.nav.helse.spion.integrasjon.pdl

import com.fasterxml.jackson.databind.ObjectMapper
import io.ktor.client.*
import io.ktor.client.request.*
import io.ktor.http.ContentType
import io.ktor.http.content.*
import kotlinx.coroutines.runBlocking
import java.io.IOException

class PdlClient(
        private val pdlUrl: String,
        private val stsClient: RestStsClient,
        private val httpClient: HttpClient,
        private val om: ObjectMapper
) {
    private val query = this::class.java.getResource("/pdl/hentPerson.graphql").readText().replace(Regex("[\n\r]"), "")

    fun person(ident: String): PdlHentPerson? {
        val stsToken = stsClient.getOidcToken()
        val entity = PdlRequest(query, Variables(ident))
        val pdlPersonReponse = runBlocking {
             httpClient.post<PdlPersonResponse> {
                url(pdlUrl)
                body = TextContent(om.writeValueAsString(entity), contentType = ContentType.Application.Json)
                header("Tema", "SYK")
                header("Authorization", "Bearer $stsToken")
                header("Nav-Consumer-Token", "Bearer $stsToken")
            }
        }

        if (pdlPersonReponse.errors != null && pdlPersonReponse.errors.isNotEmpty()) {
            val errorMessages = pdlPersonReponse.errors.map { it.message }.joinToString()
            throw IOException("Error while requesting person from PersonDataLosningen: ${errorMessages}")
        }

        return pdlPersonReponse.data
    }
}