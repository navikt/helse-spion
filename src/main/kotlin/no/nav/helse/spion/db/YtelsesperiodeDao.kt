package no.nav.helse.spion.db

import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.KotlinModule
import com.fasterxml.jackson.module.kotlin.readValue
import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import no.nav.helse.spion.domene.Arbeidsgiver
import no.nav.helse.spion.domene.Periode
import no.nav.helse.spion.domene.ytelsesperiode.Arbeidsforhold
import no.nav.helse.spion.domene.ytelsesperiode.Ytelsesperiode
import java.sql.Connection

interface YtelsesperiodeDao {
    fun save(yp : Ytelsesperiode)
}

class PostgresYtelsesperiodeDao (con: Connection) : YtelsesperiodeDao {
    val con = con
    val mapper = ObjectMapper()
            .registerModule(KotlinModule())
            .registerModule(JavaTimeModule())
            .configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false)
            .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)

    override fun save(yp: Ytelsesperiode) {
        val json = mapper.writeValueAsString(yp)
        con.prepareStatement("INSERT INTO spiondata (ytelsesperiode) VALUES ('$json')").executeUpdate()
        con.close()
    }

    fun getByIdentitetAndVirksomhet(idnr: String, vrknr: String) : List<Ytelsesperiode> {
        val sql = """SELECT ytelsesperiode::json FROM spiondata 
            WHERE ytelsesperiode -> 'arbeidsforhold' -> 'arbeidstaker' ->> 'identitetsnummer' = '$idnr'
            AND ytelsesperiode -> 'arbeidsforhold' -> 'arbeidsgiver' ->> 'virksomhetsnummer' = '$vrknr';"""

        val res = con.prepareStatement(sql).executeQuery()
        val resultatListe = ArrayList<Ytelsesperiode>()

        while (res.next()) {
            resultatListe.add(mapper.readValue(res.getString("ytelsesperiode")))
        }
        return resultatListe
    }

    fun delete(arbeidsforhold: Arbeidsforhold, p: Periode) : Int {
        val sql = """DELETE  FROM spiondata 
         WHERE ytelsesperiode -> 'arbeidsforhold' -> 'arbeidstaker' ->> 'identitetsnummer' = '${arbeidsforhold.arbeidstaker.identitetsnummer}'
            AND ytelsesperiode -> 'arbeidsforhold' -> 'arbeidsgiver' ->> 'virksomhetsnummer' = '${arbeidsforhold.arbeidsgiver.virksomhetsnummer}'
            AND ytelsesperiode -> 'periode' ->> 'fom' = '${p.fom}'
            AND ytelsesperiode -> 'periode' ->> 'tom' = '${p.tom}';"""
        val deletedCount = con.prepareStatement(sql).executeUpdate()
        con.close()
        return deletedCount
    }
}