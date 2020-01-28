package no.nav.helse.spion.web

import com.fasterxml.jackson.core.util.DefaultIndenter
import com.fasterxml.jackson.core.util.DefaultPrettyPrinter
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.KotlinModule
import io.ktor.config.ApplicationConfig
import io.ktor.util.KtorExperimentalAPI
import no.nav.helse.spion.auth.*
import no.nav.helse.spion.auth.altinn.AltinnClient
import no.nav.helse.spion.domene.sak.repository.MockSaksinformasjonRepository
import no.nav.helse.spion.domene.sak.repository.SaksinformasjonRepository
import no.nav.helse.spion.domenetjenester.SpionService
import org.koin.core.module.Module
import org.koin.dsl.module

@KtorExperimentalAPI
fun selectModuleBasedOnProfile(config: ApplicationConfig) : List<Module> {
    val envModule =  when(config.property("koin.profile").getString()) {
        "LOCAL" -> localDevConfig(config)
        "PREPROD" -> preprodConfig(config)
        "PROD" -> prodConfig(config)
        else -> localDevConfig(config)
    }
    return listOf(common, envModule)
}

val common = module {
    val om = ObjectMapper()
    om.registerModule(KotlinModule())
    om.registerModule(Jdk8Module())
    om.registerModule(JavaTimeModule())
    om.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
    om.configure(SerializationFeature.INDENT_OUTPUT, true)
    om.setDefaultPrettyPrinter(DefaultPrettyPrinter().apply {
        indentArraysWith(DefaultPrettyPrinter.FixedSpaceIndenter.instance)
        indentObjectsWith(DefaultIndenter("  ", "\n"))
    })

    single { om }
}

fun localDevConfig(config: ApplicationConfig) = module {
    single {MockSaksinformasjonRepository() as SaksinformasjonRepository}
    single {SpionService(get())}
    single {MockAuthRepo() as AuthorizationsRepository}
    single {DefaultAuthorizer(get()) as Authorizer }

    startLocalOIDCWireMock()
}

fun preprodConfig(config: ApplicationConfig) = module {
    single {MockSaksinformasjonRepository() as SaksinformasjonRepository}
    single {SpionService(get())}
    single {AltinnClient(
            config.getString("altinn.service_owner_api_url"),
            config.getString("altinn.gw_api_key"),
            config.getString("altinn.altinn_api_key"),
            config.getString("altinn.service_id")
    ) as AuthorizationsRepository}
    single {DefaultAuthorizer(get()) as Authorizer }
}


fun prodConfig(config: ApplicationConfig) = module {
    preprodConfig(config)
}

@KtorExperimentalAPI
fun ApplicationConfig.getString(path : String): String {
    return this.property(path).toString()
}