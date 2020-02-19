package no.nav.helse.spion.domene.ytelsesperiode.repository

import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.KotlinModule
import com.fasterxml.jackson.module.kotlin.readValue
import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import io.ktor.config.ApplicationConfig
import no.nav.helse.spion.db.PostgresYtelsesperiodeDao
import no.nav.helse.spion.db.createHikariConfig
import no.nav.helse.spion.domene.Arbeidsgiver
import no.nav.helse.spion.domene.ytelsesperiode.Ytelsesperiode
import javax.sql.DataSource

class PostgresRepository (ds: DataSource) : YtelsesperiodeRepository {
    val dataSource = ds

    override fun hentArbeidsgivere(identitetsnummer: String): List<Arbeidsgiver> {
        TODO("not implemented")
    }

    override fun hentYtelserForPerson(identitetsnummer: String, virksomhetsnummer: String): List<Ytelsesperiode> {
        val con = dataSource.connection
        val pg = PostgresYtelsesperiodeDao(con)
        val ytelser = pg.getByIdentitetAndVirksomhet(identitetsnummer, virksomhetsnummer)
        con.close()
        return ytelser
    }

    fun lagreYtelsesperiode(periode: Ytelsesperiode) {
        val con = dataSource.connection
        val pg = PostgresYtelsesperiodeDao(con)
        pg.save(periode)
        con.close()
    }

}