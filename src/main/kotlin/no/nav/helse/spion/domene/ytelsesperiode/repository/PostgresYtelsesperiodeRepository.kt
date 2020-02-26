package no.nav.helse.spion.domene.ytelsesperiode.repository

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import no.nav.helse.spion.domene.ytelsesperiode.Ytelsesperiode
import java.sql.Connection
import javax.sql.DataSource

class PostgresYtelsesperiodeRepository(val ds: DataSource, val mapper: ObjectMapper) : YtelsesperiodeRepository {
    private val tableName = "ytelsesperiode"
    private val getByPersonAndArbeidsgiverStatement = """SELECT data::json FROM $tableName 
            WHERE data -> 'arbeidsforhold' -> 'arbeidstaker' ->> 'identitetsnummer' = ?
            AND data -> 'arbeidsforhold' -> 'arbeidsgiver' ->> 'arbeidsgiverId' = ?;"""

    private val saveStatement = "INSERT INTO $tableName (data) VALUES (?::json);"

    private val deleteStatement = """DELETE  FROM $tableName 
         WHERE data -> 'arbeidsforhold' -> 'arbeidstaker' ->> 'identitetsnummer' = ?
            AND data -> 'arbeidsforhold' -> 'arbeidsgiver' ->> 'arbeidsgiverId' = ?
            AND data ->> 'ytelse' = ?
            AND data -> 'periode' ->> 'fom' = ?
            AND data -> 'periode' ->> 'tom' = ?;"""

    private val getStatement = """SELECT data::json FROM $tableName 
         WHERE data -> 'arbeidsforhold' -> 'arbeidstaker' ->> 'identitetsnummer' = ?
            AND data -> 'arbeidsforhold' -> 'arbeidsgiver' ->> 'arbeidsgiverId' = ?
            AND data ->> 'ytelse' = ?
            AND data -> 'periode' ->> 'fom' = ?
            AND data -> 'periode' ->> 'tom' = ?;"""


    override fun hentYtelserForPerson(identitetsnummer: String, virksomhetsnummer: String): List<Ytelsesperiode> {
        ds.connection.use { con ->

            val res = con.prepareStatement(getByPersonAndArbeidsgiverStatement).apply {
                setString(1, identitetsnummer)
                setString(2, virksomhetsnummer)
            }.executeQuery()
            val resultatListe = ArrayList<Ytelsesperiode>()

            while (res.next()) {
                resultatListe.add(mapper.readValue(res.getString("data")))
            }
            return resultatListe
        }
    }

    /** Lagrer eller erstatter ytelseperiode med mindre det allerede eksisterer en med høyere offset. */
    override fun upsert(yp: Ytelsesperiode) {
        ds.connection.use { con ->
            //TODO Transaction
            val eksisterendeYtelsesperiode = finnEksisterendeYtelsesperiode(con, yp)
            eksisterendeYtelsesperiode?.let {
                if (eksisterendeYtelsesperiode.kafkaOffset > yp.kafkaOffset)
                    return
                deleteYtelsesperiode(eksisterendeYtelsesperiode)
            }
            executeSave(yp, con)
        }
    }

    private fun executeSave(yp: Ytelsesperiode, con: Connection): Int {
        val json = mapper.writeValueAsString(yp)
        return con.prepareStatement(saveStatement).apply {
            setString(1, json)
        }.executeUpdate()
    }

    private fun finnEksisterendeYtelsesperiode(con: Connection, yp: Ytelsesperiode): Ytelsesperiode? {
        val eksisterendeYpListe = ArrayList<Ytelsesperiode>()
        val res = con.prepareStatement(getStatement).apply {
            setString(1, yp.arbeidsforhold.arbeidstaker.identitetsnummer)
            setString(2, yp.arbeidsforhold.arbeidsgiver.arbeidsgiverId)
            setString(3, yp.ytelse.toString())
            setString(4, yp.periode.fom.toString())
            setString(5, yp.periode.tom.toString())
        }.executeQuery()

        while (res.next()) {
            eksisterendeYpListe.add(mapper.readValue(res.getString("data")))
        }
        return if (eksisterendeYpListe.isNotEmpty()) {
            eksisterendeYpListe.first()
        } else null
    }

    fun deleteYtelsesperiode(periode: Ytelsesperiode): Int {
        ds.connection.use { con ->
            val deletedCount = con.prepareStatement(deleteStatement).apply {
                setString(1, periode.arbeidsforhold.arbeidstaker.identitetsnummer)
                setString(2, periode.arbeidsforhold.arbeidsgiver.arbeidsgiverId)
                setString(3, periode.ytelse.toString())
                setString(4, periode.periode.fom.toString())
                setString(5, periode.periode.tom.toString())
            }.executeUpdate()
            return deletedCount
        }
    }

}