package no.nav.helse.spion.web

import com.fasterxml.jackson.core.util.DefaultIndenter
import com.fasterxml.jackson.core.util.DefaultPrettyPrinter
import com.fasterxml.jackson.databind.MapperFeature
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.KotlinModule
import io.ktor.client.*
import io.ktor.client.engine.apache.*
import io.ktor.client.features.json.*
import io.ktor.config.*
import io.ktor.util.*
import no.nav.helse.spion.auth.*
import no.nav.helse.spion.bakgrunnsjobb.BakgrunnsjobbRepository
import no.nav.helse.spion.bakgrunnsjobb.MockBakgrunnsjobbRepository
import no.nav.helse.spion.bakgrunnsjobb.PostgresBakgrunnsjobbRepository
import no.nav.helse.spion.db.createHikariConfig
import no.nav.helse.spion.db.createLocalHikariConfig
import no.nav.helse.spion.db.getDataSource
import no.nav.helse.spion.domene.Arbeidsgiver
import no.nav.helse.spion.domene.ytelsesperiode.repository.MockYtelsesperiodeRepository
import no.nav.helse.spion.domene.ytelsesperiode.repository.PostgresYtelsesperiodeRepository
import no.nav.helse.spion.domene.ytelsesperiode.repository.YtelsesperiodeRepository
import no.nav.helse.spion.domenetjenester.SpionService
import no.nav.helse.spion.vedtaksmelding.*
import org.apache.kafka.clients.CommonClientConfigs
import org.apache.kafka.common.config.SaslConfigs
import org.koin.core.Koin
import org.koin.core.definition.Kind
import org.koin.core.module.Module
import org.koin.dsl.bind
import org.koin.dsl.module
import javax.sql.DataSource
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
    single { MockBakgrunnsjobbRepository() as BakgrunnsjobbRepository }
    single { StaticMockAuthRepo(get()) as AuthorizationsRepository } bind StaticMockAuthRepo::class
    single { DefaultAuthorizer(get()) as Authorizer }
    single { SpionService(get(), get()) }

    LocalOIDCWireMock.start()
}

fun localDevConfig(config: ApplicationConfig) = module {
    single { getDataSource(createLocalHikariConfig(), "spion", null) as DataSource }

    single { PostgresYtelsesperiodeRepository(get(), get()) as YtelsesperiodeRepository }
    single { PostgresBakgrunnsjobbRepository(get()) as BakgrunnsjobbRepository }
    single { StaticMockAuthRepo(get()) as AuthorizationsRepository }
    single { DefaultAuthorizer(get()) as Authorizer }
    single { SpionService(get(), get()) }

    single { createVedtaksMeldingKafkaMock(get()) as VedtaksmeldingProvider }

    single { VedtaksmeldingService(get(), get()) }
    single { VedtaksmeldingConsumer(get(), get(), get()) }

    LocalOIDCWireMock.start()
}

@KtorExperimentalAPI
fun preprodConfig(config: ApplicationConfig) = module {
    single {
        getDataSource(createHikariConfig(config.getjdbcUrlFromProperties()),
                config.getString("database.name"),
                config.getString("database.vault.mountpath")) as DataSource
    }

    /*single {
        AltinnClient(
                config.getString("altinn.service_owner_api_url"),
                config.getString("altinn.gw_api_key"),
                config.getString("altinn.altinn_api_key"),
                config.getString("altinn.service_id"),
                get()
        ) as AuthorizationsRepository
    }*/

    single { DynamicMockAuthRepo(get(), get()) as AuthorizationsRepository }
    single { DefaultAuthorizer(get()) as Authorizer }

    single {
        VedtaksmeldingClient(mutableMapOf(
                "bootstrap.servers" to config.getString("kafka.endpoint"),
                CommonClientConfigs.SECURITY_PROTOCOL_CONFIG to "SASL_SSL",
                SaslConfigs.SASL_MECHANISM to "PLAIN",
                SaslConfigs.SASL_JAAS_CONFIG to "org.apache.kafka.common.security.plain.PlainLoginModule required " +
                        "username=\"${config.getString("kafka.username")}\" password=\"${config.getString("kafka.password")}\";"
        ), config.getString("kafka.topicname")) as VedtaksmeldingProvider
    }


    single { VedtaksmeldingService(get(), get()) }
    single { VedtaksmeldingConsumer(get(), get(), get()) }
    single { PostgresYtelsesperiodeRepository(get(), get()) as YtelsesperiodeRepository }
    single { PostgresBakgrunnsjobbRepository(get()) as BakgrunnsjobbRepository }

    single { SpionService(get(), get()) }
}

@KtorExperimentalAPI
fun prodConfig(config: ApplicationConfig) = module {
    single {
        getDataSource(createHikariConfig(config.getjdbcUrlFromProperties()),
                config.getString("database.name"),
                config.getString("database.vault.mountpath")) as DataSource
    }

    single { StaticMockAuthRepo(get()) as AuthorizationsRepository } bind StaticMockAuthRepo::class
    single { SpionService(get(), get()) }
    single { DefaultAuthorizer(get()) as Authorizer }

    single { generateEmptyMock() as VedtaksmeldingProvider }

    single { PostgresYtelsesperiodeRepository(get(), get()) as YtelsesperiodeRepository }
    single { PostgresBakgrunnsjobbRepository(get()) as BakgrunnsjobbRepository }
    single { VedtaksmeldingService(get(), get()) }

    single { VedtaksmeldingConsumer(get(), get(), get()) }
}

val createVedtaksMeldingKafkaMock = fun(om: ObjectMapper): VedtaksmeldingProvider {
    return object : VedtaksmeldingProvider { // dum mock
        val arbeidsgivere = mutableListOf(
                Arbeidsgiver("Eltrode AS", "917346380", "917404437"),
                Arbeidsgiver("JØA OG SEL", "911366940", "711485759")
        )

        val generator = VedtaksmeldingGenerator(arbeidsgivere)
        override fun getMessagesToProcess(): List<MessageWithOffset> {
            var offset = 0.toLong()
            return if (Random.Default.nextDouble() < 0.1)
                generator.take(Random.Default.nextInt(2, 50)).map { MessageWithOffset(offset++, om.writeValueAsString(it)) }
            else emptyList()
        }

        override fun confirmProcessingDone() {
            println("KafkaMock: Comitta til kafka")
        }
    }
}

val generateEmptyMock = fun(): VedtaksmeldingProvider {
    return object : VedtaksmeldingProvider { // dum mock
        override fun getMessagesToProcess(): List<MessageWithOffset> {
            return emptyList()
        }

        override fun confirmProcessingDone() {
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
    return String.format("jdbc:postgresql://%s:%s/%s",
            this.property("database.host").getString(),
            this.property("database.port").getString(),
            this.property("database.name").getString())
}

inline fun <reified T : Any> Koin.getAllOfType(): Collection<T> =
        let { koin ->
            koin.rootScope.beanRegistry
                    .getAllDefinitions()
                    .filter { it.kind == Kind.Single }
                    .map { koin.get<Any>(clazz = it.primaryType, qualifier = null, parameters = null) }
                    .filterIsInstance<T>()
        }
