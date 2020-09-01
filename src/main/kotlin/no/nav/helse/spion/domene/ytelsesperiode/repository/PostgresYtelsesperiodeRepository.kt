package no.nav.helse.spion.domene.ytelsesperiode.repository

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import no.nav.helse.spion.domene.Periode
import no.nav.helse.spion.domene.ytelsesperiode.Ytelsesperiode
import org.slf4j.LoggerFactory
import java.sql.Connection
import java.sql.SQLException
import javax.sql.DataSource

class PostgresYtelsesperiodeRepository(val ds: DataSource, val mapper: ObjectMapper) : YtelsesperiodeRepository {
    private val logger = LoggerFactory.getLogger(PostgresYtelsesperiodeRepository::class.java)
    private val tableName = "ytelsesperiode"

    private val getByArbeidsgiverInPeriodStatement = """SELECT data::json FROM $tableName 
            WHERE data -> 'arbeidsforhold' -> 'arbeidsgiver' ->> 'arbeidsgiverId' = ?
            AND ((? <=  data -> 'periode' ->> 'fom' AND ? >=  data -> 'periode' ->> 'fom')
                OR
                (? >= data -> 'periode' ->> 'tom' AND ? <= data -> 'periode' ->> 'tom'));"""

    private val getByPersonAndArbeidsgiverStatement = """SELECT data::json FROM $tableName 
            WHERE data -> 'arbeidsforhold' -> 'arbeidstaker' ->> 'identitetsnummer' = ?
            AND data -> 'arbeidsforhold' -> 'arbeidsgiver' ->> 'arbeidsgiverId' = ?;"""

    private val getByPersonAndArbeidsgiverInPeriodStatement = """SELECT data::json FROM $tableName 
            WHERE data -> 'arbeidsforhold' -> 'arbeidstaker' ->> 'identitetsnummer' = ?
            AND data -> 'arbeidsforhold' -> 'arbeidsgiver' ->> 'arbeidsgiverId' = ?
            AND ((? <=  data -> 'periode' ->> 'fom' AND ? >=  data -> 'periode' ->> 'fom')
                OR
                (? >= data -> 'periode' ->> 'tom' AND ? <= data -> 'periode' ->> 'tom'));"""

    private val saveStatement = "INSERT INTO $tableName (data) VALUES (?::json);"

    private val deleteStatement = """DELETE  FROM $tableName 
         WHERE data -> 'arbeidsforhold' -> 'arbeidstaker' ->> 'identitetsnummer' = ?
            AND data -> 'arbeidsforhold' -> 'arbeidsgiver' ->> 'arbeidsgiverId' = ?
            AND data ->> 'ytelse' = ?
            AND data -> 'periode' ->> 'fom' = ?
            AND data -> 'periode' ->> 'tom' = ?;"""

    private val getByPersonStatement = """SELECT data::json FROM $tableName 
         WHERE data -> 'arbeidsforhold' -> 'arbeidstaker' ->> 'identitetsnummer' = ?
            AND data -> 'arbeidsforhold' -> 'arbeidsgiver' ->> 'arbeidsgiverId' = ?
            AND data ->> 'ytelse' = ?
            AND data -> 'periode' ->> 'fom' = ?
            AND data -> 'periode' ->> 'tom' = ?;"""


    override fun getYtelserForPerson(identitetsnummer: String, virksomhetsnummer: String, periode: Periode?): List<Ytelsesperiode> {
        ds.connection.use { con ->
            val resultList = ArrayList<Ytelsesperiode>()
            val res = periode?.let {
                con.prepareStatement(getByPersonAndArbeidsgiverInPeriodStatement).apply {
                    setString(1, identitetsnummer)
                    setString(2, virksomhetsnummer)
                    setString(3, periode.fom.toString())
                    setString(4, periode.tom.toString())
                    setString(5, periode.tom.toString())
                    setString(6, periode.fom.toString())
                }.executeQuery()
            } ?: run {
                con.prepareStatement(getByPersonAndArbeidsgiverStatement).apply {
                    setString(1, identitetsnummer)
                    setString(2, virksomhetsnummer)
                }.executeQuery()
            }

            while (res.next()) {
                resultList.add(mapper.readValue(res.getString("data")))
            }
            return resultList
        }
    }

    override fun getYtelserForVirksomhet(virksomhetsnummer: String, periode: Periode): List<Ytelsesperiode> {
        ds.connection.use { con ->
            val resultList = ArrayList<Ytelsesperiode>()
            val res = con.prepareStatement(getByArbeidsgiverInPeriodStatement).apply {
                setString(1, virksomhetsnummer)
                setString(2, periode.fom.toString())
                setString(3, periode.tom.toString())
                setString(4, periode.tom.toString())
                setString(5, periode.fom.toString())
            }.executeQuery()

            while (res.next()) {
                resultList.add(mapper.readValue(res.getString("data")))
            }
            return resultList
        }
    }

    /** Lagrer eller erstatter ytelseperiode med mindre det allerede eksisterer en med høyere offset. */
    override fun upsert(yp: Ytelsesperiode) {
        ds.connection.use { con ->
            try {
                con.autoCommit = false
                val existingYp = getExistingYtelsesperiode(con, yp)
                existingYp?.let {
                    if (existingYp.sistEndret.isAfter(yp.sistEndret)) //TODO Det kommer en timestamp her
                        return
                    delete(existingYp)
                }
                executeSave(yp, con)
                con.commit()
            } catch (e: SQLException) {
                logger.error("Ruller tilbake ytelsesperiode med id ${yp.vedtaksId}", e)
                try {
                    con.rollback()
                } catch (ex: Exception) {
                    logger.error("Klarte ikke rulle tilbake ytelsesperiode med id ${yp.vedtaksId}", e)
                }
            }
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
        val res = con.prepareStatement(getByPersonStatement).apply {
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
