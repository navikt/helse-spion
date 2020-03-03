package no.nav.helse.slowtests.db

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.KotlinModule
import com.fasterxml.jackson.module.kotlin.readValue
import com.zaxxer.hikari.HikariDataSource
import no.nav.helse.YtelsesperiodeGenerator
import no.nav.helse.spion.db.createLocalHikariConfig
import no.nav.helse.spion.domene.ytelsesperiode.Ytelsesperiode
import org.apache.commons.lang3.time.StopWatch
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.slf4j.LoggerFactory
import java.util.concurrent.TimeUnit
import kotlin.system.measureTimeMillis

class ReadPatternTest {

    val maxArbeidsgivere = 25000
    val maxOrg = 1000
    val maxPersoner = 1000000
    val antPerioder = 5000000
    val ds = HikariDataSource(createLocalHikariConfig())
    val mapper = ObjectMapper()
    private val logger = LoggerFactory.getLogger(ReadPatternTest::class.java)

    @BeforeEach
    fun before() {
        mapper.registerModule(KotlinModule())
        mapper.registerModule(Jdk8Module())
        mapper.registerModule(JavaTimeModule())
        mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
        mapper.configure(SerializationFeature.INDENT_OUTPUT, true)
    }


    fun genererateYtelsesperioder() {
        val bulkSize = 1000
        val numBulks = 5000
        val gen = YtelsesperiodeGenerator(maxArbeidsgivere, maxOrg, maxPersoner)
        var totaltime = 0L
        var con = ds.connection

        for (x in 0..numBulks) {
            val sql = "INSERT INTO ytelsesperiode(data) VALUES(cast (? as json))"
            var stm = con.prepareStatement(sql)
            val time = measureTimeMillis {
                for (y in 0..bulkSize) {
                    val yp = gen.next()
                    stm.setString(1, mapper.writeValueAsString(yp))
                    stm.addBatch()
                }
            }
            stm.executeBatch()
            totaltime += time
            logger.info("Spent $time ms inserting $bulkSize docs.")
        }


        logger.info("Done inserting " + numBulks * bulkSize + " items in $totaltime ms")

    }

    @Test
    fun readPatterns() {
        logger.info("Tester aktuelle read patterns i postgres.")
        val countSql = "SELECT COUNT(*) AS ypCount, COUNT(DISTINCT data ->'arbeidsforhold' -> 'arbeidstaker' ->> 'identitetsnummer') as personCount, COUNT(DISTINCT data -> 'arbeidsforhold' ->> 'arbeidsgiver') as arbeidsgiverCount, COUNT (DISTINCT data -> 'arbeidsforhold' -> 'arbeidsgiver' ->> 'organisasjonsnummer') as orgCount FROM ytelsesperiode"
        var countPerson = 0
        var countArbeidsgiver = 0
        var countOrg = 0
        var countYp = 0

//        ds.connection.use {
//            var res = it.prepareStatement(countSql).executeQuery()
//
//            if (res.next()) {
//                countPerson = res.getInt("personCount")
//                countArbeidsgiver = res.getInt("arbeidsgiverCount")
//                countOrg = res.getInt("orgCount")
//                countYp = res.getInt("ypCount")
//            }
//        }
//
//        logger.info("Tester med $countYp ytelsesperioder fordelt på $countPerson personer, $countArbeidsgiver arbeidsgivere og $countOrg organisasjoner.")

//        logger.info("identitetsnummer, arbeidsgiverId -> perioder for en person i en virksomhet:")
//        personVirksomhet("10480949806", "443760600")
//
//        logger.info("identitetsnummer, arbeidsgiverId -> perioder for en person disse virksomhetene:")
//        personVirksomheter("19618478059", listOf("231495780", "635890641", "425694159", "117946012", "558996254", "840215525"))
//
//        logger.info("identitetsnummer, organisasjonsnummer -> perioder for en person i alle virksomheter under organisasjonsnummeret:")
//        personOrgnr("10044105275", "772466661")
//
//        logger.info("arbeidsgiverId -> perioder for alle personer i en virksomhet:")
//        personerVirksomhet("840215525")
//
//        logger.info("arbeidsgiverId -> perioder for alle personer i disse virksomhetene:")
//        personerVirksomheter(listOf("840215525", "899720576"))
//
//        logger.info("organisasjonsnummer -> perioder for alle personer i alle virksomheter under organisasjonsnummeret:")
//        personerOrgnr("261892320")
//
//        logger.info("virksomhetsnummer -> perioder for alle personer i alle virksomheter:")
        val virksomheter = getArbeidsgiverIds(1000).toList()
//        logger.info("10 virksomheter")
//        personerVirksomheter(virksomheter.slice(0..10))
//        logger.info("50 virksomheter")
//        personerVirksomheter(virksomheter.slice(0..50))
//        logger.info("100 virksomheter")
//        personerVirksomheter(virksomheter.slice(0..100))
//        logger.info("1000 virksomheter")
//        personerVirksomheter(virksomheter)
//
        logger.info("virksomhetsnummer -> perioder for alle personer i 100 virksomheter med periodebegrensning:")
        personerVirksomheterPeriode(virksomheter.slice(0..100), "2020-01-01", "2020-01-02")

        logger.info("virksomhetsnummer -> perioder for alle personer i 10 virksomheter med periodebegrensning:")
        personerVirksomheterPeriode(virksomheter.slice(0..10), "2020-01-01", "2020-01-02")


        logger.info("2 virksomhetsnummer -> perioder for alle personer i 100 virksomheter med periodebegrensning:")
        personerVirksomheterPeriode2(virksomheter.slice(0..100), "2020-01-01", "2020-02-02")

        logger.info("2 virksomhetsnummer -> perioder for alle personer i 10 virksomheter med periodebegrensning:")
        personerVirksomheterPeriode2(virksomheter.slice(0..10), "2020-01-01", "2020-02-02")
    }

    fun personVirksomhet(idnr: String, vrknr: String) {
        val sql = "SELECT data FROM ytelsesperiode WHERE data -> 'arbeidsforhold' -> 'arbeidstaker' ->> 'identitetsnummer' = '$idnr' AND data -> 'arbeidsforhold' -> 'arbeidsgiver' ->> 'arbeidsgiverId' = '$vrknr';"
        doTimedQuery(sql)
    }

    fun personVirksomheter(idnr: String, virksomheter: List<String>) {
        val sql = "SELECT data FROM ytelsesperiode WHERE data -> 'arbeidsforhold' -> 'arbeidstaker' ->> 'identitetsnummer' = '$idnr' AND data -> 'arbeidsforhold' -> 'arbeidsgiver' ->> 'arbeidsgiverId' IN ${virksomheter.joinToString(prefix = "(", postfix = ")") { "'$it'" }};"
        doTimedQuery(sql)
    }

    fun personOrgnr(idnr: String, orgnr: String) {
        val sql = "SELECT data FROM ytelsesperiode WHERE data -> 'arbeidsforhold' -> 'arbeidstaker' ->> 'identitetsnummer' = '$idnr' AND data -> 'arbeidsforhold' -> 'arbeidsgiver' ->> 'organisasjonsnummer' = '$orgnr';"
        doTimedQuery(sql)
    }

    fun personerVirksomhet(vrknr: String) {
        val sql = "SELECT data FROM ytelsesperiode WHERE data -> 'arbeidsforhold' -> 'arbeidsgiver' ->> 'arbeidsgiverId' = '$vrknr';"
        doTimedQuery(sql)
    }

    fun personerVirksomheterPeriode(vrknr: List<String>, fom: String, tom: String) {
        val sql = "select data  from  ytelsesperiode where  data -> 'arbeidsforhold' -> 'arbeidsgiver' ->> 'arbeidsgiverId' IN  ${vrknr.joinToString(prefix = "(", postfix = ")") { "'$it'" }} and to_date(data -> 'periode' ->> 'fom', 'YYYY-MM-DD') between '$fom' and '$tom' or to_date(data -> 'periode' ->> 'tom', 'YYYY-MM-DD') between '$fom' and '$tom';"
        doTimedQuery(sql)
    }

    fun personerVirksomheterPeriode2(vrknr: List<String>, fom: String, tom: String) {
        val sql = "select data  from  ytelsesperiode where  data -> 'arbeidsforhold' -> 'arbeidsgiver' ->> 'arbeidsgiverId' " +
                "IN  ${vrknr.joinToString(prefix = "(", postfix = ")") { "'$it'" }}" +
                " AND( ('$fom' <=  data -> 'periode' ->> 'fom' AND '$tom' >=  data -> 'periode' ->> 'fom')" +
                "OR" +
                "('$tom' >= data -> 'periode' ->> 'tom' AND '$fom' <= data -> 'periode' ->> 'tom') );"
        doTimedQuery(sql)
    }

    fun personerVirksomheter(vrknr: List<String>) {
        val sql = "SELECT data FROM ytelsesperiode WHERE data -> 'arbeidsforhold' -> 'arbeidsgiver' ->> 'arbeidsgiverId' IN ${vrknr.joinToString(prefix = "(", postfix = ")") { "'$it'" }};"
        doTimedQuery(sql, false)
    }


    fun personerOrgnr(orgnr: String) {
        val sql = "SELECT data FROM ytelsesperiode WHERE data -> 'arbeidsforhold' -> 'arbeidsgiver' ->> 'organisasjonsnummer' = '$orgnr';"
        doTimedQuery(sql)
    }

    fun doTimedQuery(queryString: String, printQuery: Boolean = true) {
        val sw = StopWatch()
        if (printQuery) logger.info("Spørring: $queryString")
        ds.connection.use { con ->
            sw.start()
            val results = con.prepareStatement(queryString).executeQuery()
            val queryTime = sw.getTime(TimeUnit.MILLISECONDS)
            var resultatListe: MutableCollection<Ytelsesperiode> = ArrayList()
            while (results.next()) {
                resultatListe.add(mapper.readValue(results.getString("data")))
            }
            logger.info("Resultatsett med ${resultatListe.size} treff, returnerer på $queryTime ms")
            val totalTime = sw.getTime(TimeUnit.MILLISECONDS)
            logger.info("Laster inn og lager objekter på $totalTime ms")
            logger.info("-.-.-.-.-.-.-.-.-.-.-.-.-.-.-.-.-")
        }
    }

    fun getArbeidsgiverIds(max: Int): MutableCollection<String> {
        val sql = "SELECT data -> 'arbeidsforhold' -> 'arbeidsgiver' ->> 'arbeidsgiverId' AS idnr FROM ytelsesperiode GROUP BY idnr ORDER BY COUNT(*) DESC LIMIT $max;"

        ds.connection.use { con ->
            val results = con.prepareStatement(sql).executeQuery()
            var resultatListe: MutableCollection<String> = ArrayList()
            while (results.next()) {
                resultatListe.add(results.getString("idnr"))
            }
            return resultatListe
        }
    }

}