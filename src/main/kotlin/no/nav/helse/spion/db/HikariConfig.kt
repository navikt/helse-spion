package no.nav.helse.spion.db

import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import io.ktor.application.Application
import io.ktor.util.KtorExperimentalAPI

fun createHikariConfig(jdbcUrl: String, username: String? = null, password: String? = null) =
    HikariConfig().apply {
        this.jdbcUrl = jdbcUrl
        maximumPoolSize = 3
        minimumIdle = 1
        idleTimeout = 10001
        connectionTimeout = 1000
        maxLifetime = 30001
        driverClassName = "org.postgresql.Driver"
        username?.let { this.username = it }
        password?.let { this.password = it }
    }

@KtorExperimentalAPI
fun Application.createHikariConfigFromEnvironment() =
    createHikariConfig(
        jdbcUrl = String.format("jdbc:postgresql://%s:%s/%s%s",
                environment.config.property("database.host").getString(),
                environment.config.property("database.port").getString(),
                environment.config.property("database.name").getString(),
                environment.config.propertyOrNull("database.username")?.getString()?.let {"?user=$it"}),
        username = environment.config.propertyOrNull("database.username")?.getString(),
        password = environment.config.propertyOrNull("database.password")?.getString()
    )

@KtorExperimentalAPI
fun Application.hikariConfig(): HikariDataSource? {
    migrate(createHikariConfigFromEnvironment())
    return getDataSource(createHikariConfigFromEnvironment())
}