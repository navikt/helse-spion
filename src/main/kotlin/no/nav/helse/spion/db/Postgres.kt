package no.nav.helse.spion.db

import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import io.ktor.application.Application
import io.ktor.config.ApplicationConfig
import io.ktor.util.KtorExperimentalAPI
import no.nav.vault.jdbc.hikaricp.HikariCPVaultUtil


enum class Role {
    Admin, User, ReadOnly;

    override fun toString() = name.toLowerCase()
}


fun getDataSource(hikariConfig: HikariConfig, dbName: String, vaultMountpath: String?) =
        if (!vaultMountpath.isNullOrEmpty()) {
            dataSourceFromVault(hikariConfig, dbName, vaultMountpath, Role.User)
        } else {
            HikariDataSource(hikariConfig)
        }

@KtorExperimentalAPI
fun dataSourceFromVault(hikariConfig: HikariConfig, dbName: String, vaultMountpath: String, role: Role) =
        HikariCPVaultUtil.createHikariDataSourceWithVaultIntegration(
                hikariConfig,
                vaultMountpath,
                "${dbName}-$role")


