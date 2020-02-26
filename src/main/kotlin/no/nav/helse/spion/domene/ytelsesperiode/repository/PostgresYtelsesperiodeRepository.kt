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


    override fun getYtelserForPerson(identitetsnummer: String, virksomhetsnummer: String): List<Ytelsesperiode> {
        ds.connection.use { con ->

            val res = con.prepareStatement(getByPersonAndArbeidsgiverStatement).apply {
                setString(1, identitetsnummer)
                setString(2, virksomhetsnummer)
            }.executeQuery()
            val resultList = ArrayList<Ytelsesperiode>()

            while (res.next()) {
                resultList.add(mapper.readValue(res.getString("data")))
            }
            return resultList
        }
    }

    /** Lagrer eller erstatter ytelseperiode med mindre det allerede eksisterer en med høyere offset. */
    override fun upsert(yp: Ytelsesperiode) {
        ds.connection.use { con ->
            con.autoCommit = false
            val existingYp = getExistingYtelsesperiode(con, yp)
            existingYp?.let {
                if (existingYp.kafkaOffset > yp.kafkaOffset)
                    return
                delete(existingYp)
            }
            executeSave(yp, con)
            con.commit()
        }
    }

     fun executeSave(yp: Ytelsesperiode, con: Connection): Int {
        val json = mapper.writeValueAsString(yp)
        return con.prepareStatement(saveStatement).apply {
            setString(1, json)
        }.executeUpdate()
    }

    private fun getExistingYtelsesperiode(con: Connection, yp: Ytelsesperiode): Ytelsesperiode? {
        val existingYpList = ArrayList<Ytelsesperiode>()
        val res = con.prepareStatement(getStatement).apply {
            setString(1, yp.arbeidsforhold.arbeidstaker.identitetsnummer)
            setString(2, yp.arbeidsforhold.arbeidsgiver.arbeidsgiverId)
            setString(3, yp.ytelse.toString())
            setString(4, yp.periode.fom.toString())
            setString(5, yp.periode.tom.toString())
        }.executeQuery()

        while (res.next()) {
            existingYpList.add(mapper.readValue(res.getString("data")))
        }
        return if (existingYpList.isNotEmpty()) {
            existingYpList.first()
        } else null
    }

    fun delete(yp: Ytelsesperiode): Int {
        ds.connection.use { con ->
            val deletedCount = con.prepareStatement(deleteStatement).apply {
                setString(1, yp.arbeidsforhold.arbeidstaker.identitetsnummer)
                setString(2, yp.arbeidsforhold.arbeidsgiver.arbeidsgiverId)
                setString(3, yp.ytelse.toString())
                setString(4, yp.periode.fom.toString())
                setString(5, yp.periode.tom.toString())
            }.executeUpdate()
            return deletedCount
        }
    }

}