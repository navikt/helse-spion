package no.nav.helse.spion.web

import io.ktor.config.MapApplicationConfig
import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty
import io.ktor.util.KtorExperimentalAPI
import org.slf4j.LoggerFactory
import java.util.concurrent.TimeUnit


@KtorExperimentalAPI
fun main() {
    Thread.currentThread().setUncaughtExceptionHandler { thread, err ->
        LoggerFactory.getLogger("main")
            .error("uncaught exception in thread ${thread.name}: ${err.message}", err)
    }

    val config = readDevProps()

    embeddedServer(Netty, createApplicationEnvironment(config)).let { app ->
        app.start(wait = false)

        Runtime.getRuntime().addShutdownHook(Thread {
            app.stop(1, 1, TimeUnit.SECONDS)
        })
    }
}


@KtorExperimentalAPI
fun readDevProps() =
    MapApplicationConfig().apply {
        object {}?.javaClass?.getResource("dev.props")?.readText()?.lines()?.map {
            Pair(it.split("=")[0], it.split("=")[1])
        }?.forEach { (k, v) -> put(k, v) }
    }