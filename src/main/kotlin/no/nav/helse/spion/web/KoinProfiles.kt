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
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import no.nav.helse.spion.auth.*
import no.nav.helse.spion.auth.altinn.AltinnClient
import no.nav.helse.spion.db.createHikariConfig
import no.nav.helse.spion.db.createLocalHikariConfig
import no.nav.helse.spion.db.getDataSource
import no.nav.helse.spion.domene.ytelsesperiode.repository.MockYtelsesperiodeRepository
import no.nav.helse.spion.domene.ytelsesperiode.repository.PostgresRepository
import no.nav.helse.spion.domene.ytelsesperiode.repository.YtelsesperiodeRepository
import no.nav.helse.spion.domenetjenester.SpionService
import no.nav.helse.spion.vedtaksmelding.*
import org.koin.core.Koin
import org.koin.core.definition.Kind
import org.koin.core.module.Module
import org.koin.dsl.bind
import org.koin.dsl.module
import kotlin.random.Random


@KtorExperimentalAPI
fun selectModuleBasedOnProfile(config: ApplicationConfig): List<Module> {
    val envModule = when (config.property("koin.profile").getString()) {
        "TEST" -> buildAndTestConfig()
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
    om.configure(MapperFeature.ACCEPT_CASE_INSENSITIVE_PROPERTIES, true)

    om.setDefaultPrettyPrinter(DefaultPrettyPrinter().apply {
        indentArraysWith(DefaultPrettyPrinter.FixedSpaceIndenter.instance)
        indentObjectsWith(DefaultIndenter("  ", "\n"))
    })

    single { om }

    val httpClient = HttpClient(Apache) {
        install(JsonFeature) {
            serializer = JacksonSerializer {
                configure(MapperFeature.ACCEPT_CASE_INSENSITIVE_PROPERTIES, true)
            }
        }
    }

    single { httpClient }

}

fun buildAndTestConfig() = module {
    single { MockYtelsesperiodeRepository() as YtelsesperiodeRepository }
    single { MockAuthRepo(get()) as AuthorizationsRepository } bind MockAuthRepo::class
    single { DefaultAuthorizer(get()) as Authorizer }
    single { SpionService(get(), get()) }

    LocalOIDCWireMock.start()
}

fun localDevConfig(config: ApplicationConfig) = module {
    single { PostgresRepository(getDataSource(createLocalHikariConfig(), "spion", null), get()) as YtelsesperiodeRepository }
    single { MockAuthRepo(get()) as AuthorizationsRepository } bind MockAuthRepo::class
    single { DefaultAuthorizer(get()) as Authorizer }
    single { SpionService(get(), get()) }

    single { generateKafkaMock(get()) as KafkaMessageProvider }

    single { MockFailedVedtaksmeldingsRepository() as FailedVedtaksmeldingRepository }
    single { VedtaksmeldingProcessor(get(), get(), get(), get(), CoroutineScope(Dispatchers.IO), 30000) }

    LocalOIDCWireMock.start()
}

@KtorExperimentalAPI
fun preprodConfig(config: ApplicationConfig) = module {
    single { SpionService(get(), get()) }
    single {
        AltinnClient(
                config.getString("altinn.service_owner_api_url"),
                config.getString("altinn.gw_api_key"),
                config.getString("altinn.altinn_api_key"),
                config.getString("altinn.service_id"),
                get()
        ) as AuthorizationsRepository
    }
    single { DefaultAuthorizer(get()) as Authorizer }

    single { generateKafkaMock(get()) as KafkaMessageProvider }

    single { MockFailedVedtaksmeldingsRepository() as FailedVedtaksmeldingRepository }
    single { VedtaksmeldingProcessor(get(), get(), get(), get(), CoroutineScope(Dispatchers.IO), 30000) }
    single { PostgresRepository(getDataSource(createHikariConfig(config.getjdbcUrlFromProperties(),
                config.getString("database.username"),
                config.getString("database.password")),
                config.getString("database.name"),
                config.getString("database.vault.mountpath")), get()) as YtelsesperiodeRepository
    }
}

fun prodConfig(config: ApplicationConfig) = module {
    single { MockAuthRepo(get()) as AuthorizationsRepository } bind MockAuthRepo::class
    single { SpionService(get(), get()) }
    single { DefaultAuthorizer(get()) as Authorizer }

    single { generateKafkaMock(get()) as KafkaMessageProvider }
    single { MockFailedVedtaksmeldingsRepository() as FailedVedtaksmeldingRepository }

    single { VedtaksmeldingProcessor(get(), get(), get(), get(), CoroutineScope(Dispatchers.IO), 30000) }
    single {
        PostgresRepository(getDataSource(createHikariConfig(config.getjdbcUrlFromProperties(),
                config.getString("database.username"),
                config.getString("database.password")),
                config.getString("database.name"),
                config.getString("database.vault.mountpath")), get()) as YtelsesperiodeRepository
    }
}

val generateKafkaMock = fun(om: ObjectMapper): KafkaMessageProvider {
    return object : KafkaMessageProvider { // dum mock
        val generator = VedtaksmeldingGenerator(100, 1000)
        override fun getMessagesToProcess(): List<String> {
            return if (Random.Default.nextDouble() < 0.1)
                generator.take(Random.Default.nextInt(2, 50)).map { om.writeValueAsString(it) }
            else emptyList()
        }

        override fun confirmProcessingDone() {
            println("KafkaMock: Comitta til kafka")
        }
    }
}

// utils
@KtorExperimentalAPI
fun ApplicationConfig.getString(path: String): String {
    return this.property(path).getString()
}

@KtorExperimentalAPI
fun ApplicationConfig.getjdbcUrlFromProperties(): String {
    return String.format("jdbc:postgresql://%s:%s/%s%s",
            this.property("database.host").getString(),
            this.property("database.name").getString(),
            this.property("database.port").getString(),
            this.propertyOrNull("database.username")?.getString()?.let { "?user=$it" })
}

inline fun <reified T : Any> Koin.getAllOfType(): Collection<T> =
        let { koin ->
            koin.rootScope.beanRegistry
                    .getAllDefinitions()
                    .filter { it.kind == Kind.Single }
                    .map { koin.get<Any>(clazz = it.primaryType, qualifier = null, parameters = null) }
                    .filterIsInstance<T>()
        }