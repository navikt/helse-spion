package no.nav.helse.spion.web.auth

import io.ktor.config.ApplicationConfig
import io.ktor.request.ApplicationRequest
import no.nav.security.token.support.core.jwt.JwtToken
import java.time.Instant
import java.util.Date

fun hentIdentitetsnummerFraLoginToken(config: ApplicationConfig, request: ApplicationRequest): String {
    val tokenString = getTokenString(config, request)
    return JwtToken(tokenString).subject
}

fun hentUtløpsdatoFraLoginToken(config: ApplicationConfig, request: ApplicationRequest): Date {
    val tokenString = getTokenString(config, request)
    return JwtToken(tokenString).jwtTokenClaims.expirationTime ?: Date.from(Instant.MIN)
}

private fun getTokenString(config: ApplicationConfig, request: ApplicationRequest): String {
    val cookieName = config.configList("no.nav.security.jwt.issuers")[0].property("cookie_name").getString()

    return request.cookies[cookieName]
        ?: request.headers["Authorization"]?.replaceFirst("Bearer ", "")
        ?: throw IllegalAccessException("Du må angi et identitetstoken som cookieen $cookieName eller i Authorization-headeren")
}
