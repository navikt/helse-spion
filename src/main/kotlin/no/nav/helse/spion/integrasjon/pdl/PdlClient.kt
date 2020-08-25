package no.nav.helse.spion.integrasjon.pdl

import com.fasterxml.jackson.databind.ObjectMapper
import io.ktor.client.*
import io.ktor.client.request.*
import io.ktor.http.ContentType
import io.ktor.http.content.*
import kotlinx.coroutines.runBlocking
import org.slf4j.LoggerFactory

class PdlClient(
        private val pdlUrl: String,
        private val stsClient: RestStsClient,
        private val httpClient: HttpClient,
        private val om: ObjectMapper
) {
    private val query = this::class.java.getResource("/pdl/hentPerson.graphql").readText().replace(Regex("[\n\r]"), "")

    init {
        LOG.debug("Query: $query")
    }

    fun person(ident: String): PdlHentPerson? {
        val stsToken = stsClient.getOidcToken()
        val entity = PdlRequest(query, Variables(ident))
        try {
            val pdlPersonReponse = runBlocking {
                 httpClient.post<PdlPersonResponse> {
                    url(pdlUrl)
                    body = TextContent(om.writeValueAsString(entity), contentType = ContentType.Application.Json)
                    header("Tema", "SYK")
                    header("Authorization", "Bearer $stsToken")
                    header("Nav-Consumer-Token", "Bearer $stsToken")
                }
            }

            return if (pdlPersonReponse.errors != null && pdlPersonReponse.errors.isNotEmpty()) {
                pdlPersonReponse.errors.forEach {
                    LOG.error("Error while requesting person from PersonDataLosningen: ${it.errorMessage()}")
                }
                null
            } else {
                pdlPersonReponse.data
            }
        } catch (exception: Exception) {
            LOG.error("Error from PDL with request-url: $pdlUrl", exception)
            throw exception
        }
    }

    fun personName(ident: String): String? {
        return person(ident)?.fullName()
    }

    fun isKode6or7(ident: String): Boolean {
        return person(ident)?.isKode6or7() ?: true
    }

    companion object {
        private val LOG = LoggerFactory.getLogger(PdlClient::class.java)
    }
}