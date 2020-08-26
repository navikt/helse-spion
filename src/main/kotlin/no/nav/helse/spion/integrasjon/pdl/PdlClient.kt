package no.nav.helse.spion.integrasjon.pdl

import com.fasterxml.jackson.databind.ObjectMapper
import io.ktor.client.*
import io.ktor.client.request.*
import io.ktor.http.ContentType
import io.ktor.http.content.*
import kotlinx.coroutines.runBlocking
import java.io.IOException

interface NameProvider {
    data class Name(val firstname: String, val lastname: String)
    fun fnrToName(fnr: String): Name?
}

class PdlClient(
        private val pdlUrl: String,
        private val stsClient: RestStsClient,
        private val httpClient: HttpClient,
        private val om: ObjectMapper
): NameProvider {
    private val query = this::class.java.getResource("/pdl/hentPerson.graphql").readText().replace(Regex("[\n\r]"), "")

    private fun person(ident: String): PdlHentPerson? {
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
            val errorMessages = pdlPersonReponse.errors.map { it.errorMessage() }.joinToString()
            throw IOException("Error while requesting person from PersonDataLosningen: ${errorMessages}")
        }

        return pdlPersonReponse.data
    }

    override fun fnrToName(fnr: String): NameProvider.Name? {
        val name = person(fnr)
                ?.hentPerson
                ?.navn
                ?.firstOrNull()

        return if (name == null) null else NameProvider.Name(name.fornavn, name.etternavn)
    }
}