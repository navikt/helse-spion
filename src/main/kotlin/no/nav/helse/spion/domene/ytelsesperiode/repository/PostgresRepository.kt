package no.nav.helse.spion.domene.ytelsesperiode.repository

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import no.nav.helse.spion.domene.Arbeidsgiver
import no.nav.helse.spion.domene.ytelsesperiode.Ytelsesperiode
import javax.sql.DataSource

class PostgresRepository(val ds: DataSource, val mapper: ObjectMapper) : YtelsesperiodeRepository {

    override fun hentArbeidsgivere(identitetsnummer: String): List<Arbeidsgiver> {
        TODO("not implemented")
    }

    override fun hentYtelserForPerson(identitetsnummer: String, virksomhetsnummer: String): List<Ytelsesperiode> {
        ds.connection.use { con ->
            val sql = """SELECT ytelsesperiode::json FROM spiondata 
            WHERE ytelsesperiode -> 'arbeidsforhold' -> 'arbeidstaker' ->> 'identitetsnummer' = ?
            AND ytelsesperiode -> 'arbeidsforhold' -> 'arbeidsgiver' ->> 'virksomhetsnummer' = ?;"""

            val res = con.prepareStatement(sql).apply{
                setString(1, identitetsnummer)
                setString(2, virksomhetsnummer)
            }.executeQuery()
            val resultatListe = ArrayList<Ytelsesperiode>()

            while (res.next()) {
                resultatListe.add(mapper.readValue(res.getString("ytelsesperiode")))
            }
            return resultatListe
        }
    }

    override fun save(yp: Ytelsesperiode) {
        ds.connection.use { con ->

            val json = mapper.writeValueAsString(yp)
            con.prepareStatement("INSERT INTO spiondata (ytelsesperiode) VALUES (?::json)").apply{
                setString(1, json)
            }.executeUpdate()
        }
    }



    fun deleteYtelsesperiode(periode: Ytelsesperiode): Int {
        ds.connection.use { con ->
            val sql = """DELETE  FROM spiondata 
         WHERE ytelsesperiode -> 'arbeidsforhold' -> 'arbeidstaker' ->> 'identitetsnummer' = ?
            AND ytelsesperiode -> 'arbeidsforhold' -> 'arbeidsgiver' ->> 'virksomhetsnummer' = ?
            AND ytelsesperiode -> 'periode' ->> 'fom' = ?
            AND ytelsesperiode -> 'periode' ->> 'tom' = ?;"""
            val deletedCount = con.prepareStatement(sql).apply {
                setString(1, periode.arbeidsforhold.arbeidstaker.identitetsnummer)
                setString(2, periode.arbeidsforhold.arbeidsgiver.virksomhetsnummer)
                setString(3, periode.periode.fom.toString())
                setString(4, periode.periode.tom.toString())
            }.executeUpdate()
            return deletedCount
        }
    }

}