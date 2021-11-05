package no.nav.helse.spion.web

import com.typesafe.config.ConfigFactory
import io.ktor.config.HoconApplicationConfig
import io.ktor.server.engine.applicationEngineEnvironment
import io.ktor.server.engine.connector
import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty
import io.ktor.server.netty.NettyApplicationEngine
import no.nav.helse.arbeidsgiver.bakgrunnsjobb.BakgrunnsjobbService
import no.nav.helse.arbeidsgiver.kubernetes.KubernetesProbeManager
import no.nav.helse.arbeidsgiver.kubernetes.LivenessComponent
import no.nav.helse.arbeidsgiver.kubernetes.ReadynessComponent
import no.nav.helse.arbeidsgiver.system.AppEnv
import no.nav.helse.arbeidsgiver.system.getEnvironment
import no.nav.helse.arbeidsgiver.system.getString
import no.nav.helse.spion.vedtaksmelding.VedtaksmeldingConsumer
import no.nav.helse.spion.vedtaksmelding.VedtaksmeldingProcessor
import no.nav.helse.spion.web.auth.localCookieDispenser
import org.flywaydb.core.Flyway
import org.koin.core.component.KoinComponent
import org.koin.core.component.get
import org.koin.core.context.GlobalContext
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import org.slf4j.LoggerFactory

class SpionApplication(val port: Int = 8080) : KoinComponent {
    private val logger = LoggerFactory.getLogger(SpionApplication::class.simpleName)
    private var webserver: NettyApplicationEngine? = null
    private var appConfig: HoconApplicationConfig = HoconApplicationConfig(ConfigFactory.load())
    private val runtimeEnvironment = appConfig.getEnvironment()

    fun start() {
        if (runtimeEnvironment == AppEnv.PREPROD || runtimeEnvironment == AppEnv.PROD) {
            logger.info("Sover i 30s i påvente av SQL proxy sidecar")
            Thread.sleep(30000)
        }

        startKoin { modules(selectModuleBasedOnProfile(appConfig)) }
        migrateDatabase()

        configAndStartBackgroundWorker()
        autoDetectProbeableComponents()
        configAndStartWebserver()
    }

    fun shutdown() {
        webserver?.stop(1000, 1000)
        get<BakgrunnsjobbService>().stop()
        get<VedtaksmeldingConsumer>().stop()
        stopKoin()
    }

    private fun configAndStartWebserver() {
        webserver = embeddedServer(
            Netty,
            applicationEngineEnvironment {
                config = appConfig
                connector {
                    port = this@SpionApplication.port
                }

                module {
                    if (runtimeEnvironment != AppEnv.PROD) {
                        localCookieDispenser(config)
                    }
                    spionModule(config)
                }
            }
        )

        webserver!!.start(wait = false)
    }

    private fun configAndStartBackgroundWorker() {
        if (appConfig.getString("run_background_workers") == "true") {

            get<VedtaksmeldingConsumer>().apply {
                startAsync(true)
            }

            get<BakgrunnsjobbService>().apply {
                registrer(get<VedtaksmeldingProcessor>())
                startAsync(true)
            }
        }
    }

    private fun migrateDatabase() {
        logger.info("Starter databasemigrering")

        // gir get() riktig koin context?
        Flyway.configure().baselineOnMigrate(true)
            .dataSource(GlobalContext.get().get())
            .load()
            .migrate()

        logger.info("Databasemigrering slutt")
    }

    private fun autoDetectProbeableComponents() {
        val kubernetesProbeManager = get<KubernetesProbeManager>()

        getKoin().getAll<LivenessComponent>()
            .forEach { kubernetesProbeManager.registerLivenessComponent(it) }

        getKoin().getAll<ReadynessComponent>()
            .forEach { kubernetesProbeManager.registerReadynessComponent(it) }

        logger.info("La til probeable komponenter")
    }
}

fun main() {

    val logger = LoggerFactory.getLogger("main")
    Thread.currentThread().setUncaughtExceptionHandler { thread, err ->
        logger.error("uncaught exception in thread ${thread.name}: ${err.message}", err)
    }

    val application = SpionApplication()
    application.start()

    Runtime.getRuntime().addShutdownHook(
        Thread {
            logger.info("Fikk shutdown-signal, avslutter...")
            application.shutdown()
            logger.info("Avsluttet OK")
        }
    )
}
