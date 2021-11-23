package no.nav.helse.spion.koin

import io.ktor.config.ApplicationConfig
import no.nav.helse.arbeidsgiver.integrasjoner.AccessTokenProvider
import no.nav.helse.arbeidsgiver.integrasjoner.OAuth2TokenProvider
import no.nav.helse.arbeidsgiver.integrasjoner.pdl.PdlClient
import no.nav.helse.arbeidsgiver.integrasjoner.pdl.PdlClientImpl
import no.nav.helse.spion.auth.AuthorizationsRepository
import no.nav.helse.spion.auth.altinn.AltinnClient
import no.nav.helse.spion.integrasjon.oauth2.DefaultOAuth2HttpClient
import no.nav.helse.spion.integrasjon.oauth2.OAuth2ClientPropertiesConfig
import no.nav.helse.spion.integrasjon.oauth2.TokenResolver
import no.nav.helse.spion.web.getString
import no.nav.security.token.support.client.core.oauth2.ClientCredentialsTokenClient
import no.nav.security.token.support.client.core.oauth2.OAuth2AccessTokenService
import no.nav.security.token.support.client.core.oauth2.OnBehalfOfTokenClient
import no.nav.security.token.support.client.core.oauth2.TokenExchangeClient
import org.koin.core.module.Module
import org.koin.core.qualifier.named
import org.koin.dsl.bind
fun Module.externalSystemClients(config: ApplicationConfig) {
    val accessTokenProviderError = "Fant ikke config i application.conf"

    single {
        AltinnClient(
            config.getString("altinn.service_owner_api_url"),
            config.getString("altinn.gw_api_key"),
            config.getString("altinn.altinn_api_key"),
            config.getString("altinn.service_id"),
            get()
        ) as AuthorizationsRepository
    }

    single(named("PROXY")) {
        val clientConfig = OAuth2ClientPropertiesConfig(config, "proxyscope")
        val tokenResolver = TokenResolver()
        val oauthHttpClient = DefaultOAuth2HttpClient(get())
        val accessTokenService = OAuth2AccessTokenService(
            tokenResolver,
            OnBehalfOfTokenClient(oauthHttpClient),
            ClientCredentialsTokenClient(oauthHttpClient),
            TokenExchangeClient(oauthHttpClient)
        )
        val azureAdConfig = clientConfig.clientConfig["azure_ad"] ?: error(accessTokenProviderError)
        OAuth2TokenProvider(accessTokenService, azureAdConfig)
    } bind AccessTokenProvider::class

    single { PdlClientImpl(config.getString("pdl_url"), get(qualifier = named("PROXY")), get(), get()) } bind PdlClient::class
}
