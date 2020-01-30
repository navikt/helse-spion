package no.nav.helse.spion.web

import com.fasterxml.jackson.core.util.DefaultIndenter
import com.fasterxml.jackson.core.util.DefaultPrettyPrinter
import com.fasterxml.jackson.databind.MapperFeature
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.KotlinModule
import io.ktor.client.HttpClient
import io.ktor.client.engine.apache.Apache
import io.ktor.client.features.json.JacksonSerializer
import io.ktor.client.features.json.JsonFeature
import io.ktor.config.ApplicationConfig
import io.ktor.util.KtorExperimentalAPI
import no.nav.helse.spion.auth.*
import no.nav.helse.spion.auth.altinn.AltinnClient
import no.nav.helse.spion.domene.sak.repository.MockSaksinformasjonRepository
import no.nav.helse.spion.domene.sak.repository.SaksinformasjonRepository
import no.nav.helse.spion.domenetjenester.SpionService
import org.koin.core.Koin
import org.koin.core.definition.Kind
import org.koin.core.module.Module
import org.koin.dsl.bind
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

    val httpClient = HttpClient(Apache) {
        install(JsonFeature) { serializer = JacksonSerializer {
            configure(MapperFeature.ACCEPT_CASE_INSENSITIVE_PROPERTIES, true)
        } }
    }

    single { httpClient }
}

fun localDevConfig(config: ApplicationConfig) = module {
    single {MockSaksinformasjonRepository() as SaksinformasjonRepository}
    single {SpionService(get())}
    single {MockAuthRepo() as AuthorizationsRepository} bind MockAuthRepo::class
    single {DefaultAuthorizer(get()) as Authorizer }

    LocalOIDCWireMock.start()
}

fun preprodConfig(config: ApplicationConfig) = module {
    single {MockSaksinformasjonRepository() as SaksinformasjonRepository}
    single {SpionService(get())}
    single {AltinnClient(
            config.getString("altinn.service_owner_api_url"),
            config.getString("altinn.gw_api_key"),
            config.getString("altinn.altinn_api_key"),
            config.getString("altinn.service_id"),
            get()
    ) as AuthorizationsRepository}
    single {DefaultAuthorizer(get()) as Authorizer }
}


fun prodConfig(config: ApplicationConfig) = preprodConfig(config).apply {

}


// utils

@KtorExperimentalAPI
fun ApplicationConfig.getString(path : String): String {
    return this.property(path).toString()
}

inline fun <reified T : Any> Koin.getAllOfType(): Collection<T> =
        let { koin ->
            koin.rootScope.beanRegistry
                    .getAllDefinitions()
                    .filter { it.kind == Kind.Single }
                    .map { koin.get<Any>(clazz = it.primaryType, qualifier = null, parameters = null) }
                    .filterIsInstance<T>()
        }