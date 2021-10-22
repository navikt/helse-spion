package no.nav.helse.spion.integrasjon.pdl

import com.fasterxml.jackson.databind.ObjectMapper
import com.nimbusds.jwt.JWT
import com.nimbusds.jwt.JWTParser
import org.slf4j.LoggerFactory
import java.net.HttpURLConnection
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.time.Duration
import java.time.Instant
import java.util.*

class RestStsClient(username: String, password: String, stsEndpoint: String) {

    private val httpClient: HttpClient
    private val endpointURI: URI
    private val basicAuth: String

    private var currentToken: JwtToken

    init {
        basicAuth = basicAuth(username, password)
        endpointURI = URI.create("$stsEndpoint?grant_type=client_credentials&scope=openid")
        httpClient = HttpClient.newHttpClient()
        currentToken = requestToken()
    }

    fun getOidcToken(): String {
        if (isExpired(currentToken, Date.from(Instant.now().plusSeconds(300)))) {
            log.info("OIDC Token is expired, getting a new one from the STS")
            currentToken = requestToken()
            log.info("Hentet nytt token fra sts som går ut ${currentToken.expirationTime}")
        }
        return currentToken.tokenAsString
    }

    private fun requestToken(): JwtToken {
        log.info("sts endpoint uri: $endpointURI")
        val request = HttpRequest.newBuilder()
            .uri(endpointURI)
            .header("Authorization", basicAuth)
            .header("Accept", "application/json")
            .timeout(Duration.ofSeconds(10))
            .GET()
            .build()

        val response = httpClient.send(request, HttpResponse.BodyHandlers.ofString())
        check(response.statusCode() == HttpURLConnection.HTTP_OK) { String.format("Feil oppsto under henting av token fra STS - %s", response.body()) }

        val accessToken = ObjectMapper().readValue(response.body(), STSOidcResponse::class.java).access_token
            ?: throw IllegalStateException("Feilet ved kall til STS, ingen access token returnert")

        return JwtToken(accessToken)
    }

    private fun basicAuth(username: String, password: String): String {
        log.info("basic auth username: $username")
        return "Basic " + Base64.getEncoder().encodeToString("$username:$password".toByteArray())
    }

    companion object {
        private val log = LoggerFactory.getLogger(RestStsClient::class.java)
    }
}

fun isExpired(jwtToken: JwtToken, date: Date): Boolean {
    return date.after(jwtToken.expirationTime) &&
        jwtToken.expirationTime.before(date)
}

class STSOidcResponse {
    var access_token: String? = null
    var token_type: String? = null
    var expires_in: Int? = null
}

class JwtToken(encodedToken: String) {
    val tokenAsString: String = encodedToken
    val jwt: JWT = JWTParser.parse(encodedToken)
    val expirationTime = jwt.jwtClaimsSet.expirationTime
}
