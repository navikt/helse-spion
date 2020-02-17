package no.nav.helse.spion.db.integration

import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.KotlinModule
import com.fasterxml.jackson.module.kotlin.readValue
import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import no.nav.helse.YtelsesperiodeGenerator
import no.nav.helse.spion.domene.Arbeidsgiver
import no.nav.helse.spion.domene.Periode
import no.nav.helse.spion.domene.Person
import no.nav.helse.spion.domene.ytelsesperiode.Arbeidsforhold
import no.nav.helse.spion.domene.ytelsesperiode.Ytelsesperiode
import org.apache.commons.lang3.time.StopWatch
import org.junit.jupiter.api.Test
import java.math.BigDecimal
import java.time.LocalDate
import java.util.concurrent.TimeUnit
import kotlin.system.measureTimeMillis
import kotlin.test.assertEquals

class postgresTests {

    val config = HikariConfig().apply {
        jdbcUrl = "jdbc:postgresql://localhost:5432/spion"
        maximumPoolSize = 3
        minimumIdle = 1
        idleTimeout = 10001
        connectionTimeout = 1000
        maxLifetime = 30001
        driverClassName = "org.postgresql.Driver"
        username = "spion"
        password = "spion"
    }

    val testYtelsesPeriode = Ytelsesperiode(
            periode = Periode(LocalDate.of(2019, 1, 1), LocalDate.of(2019, 2, 1)),
            arbeidsforhold = Arbeidsforhold(
                    arbeidsforholdId = "1",
                    arbeidstaker = Person("Solan", "Gundersen", "10987654321"),
                    arbeidsgiver = Arbeidsgiver("Flåklypa Verksted", "666666666", "555555555", null)
            ),
            vedtaksId = "1",
            refusjonsbeløp = BigDecimal(10000),
            status = Ytelsesperiode.Status.INNVILGET,
            grad = BigDecimal(50),
            dagsats = BigDecimal(200),
            maxdato = LocalDate.of(2019, 1, 1),
            ferieperioder = emptyList(),
            ytelse = Ytelsesperiode.Ytelse.SP,
            merknad = "Fritak fra AGP",
            sistEndret = LocalDate.now()
    )
    val mapper = ObjectMapper().registerModule(KotlinModule()).registerModule(JavaTimeModule()).configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false).configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)

    val testJson = mapper.writeValueAsString(testYtelsesPeriode)

    val dataSource =  HikariDataSource(config)

    @Test
    fun `Lagrer en ytelsesperiode`() {
        val con = dataSource.connection
        con.prepareStatement("INSERT INTO spiondata (ytelsesperiode) VALUES ('$testJson')").executeUpdate()

        val res = con.prepareStatement("SELECT ytelsesperiode::json from spiondata where ytelsesperiode -> 'arbeidsforhold' -> 'arbeidsgiver' ->> 'virksomhetsnummer' = '555555555';").executeQuery()
        res.next()
        val responsString = res.getString("ytelsesperiode")

        val yp: Ytelsesperiode = mapper.readValue(responsString)
        assertEquals(testYtelsesPeriode, yp)
        assertEquals(testYtelsesPeriode.arbeidsforhold.arbeidsforholdId, yp.arbeidsforhold.arbeidsforholdId)


        //cleanup
        con.prepareStatement("DELETE  FROM spiondata WHERE id = 1").executeUpdate()
        con.close()


    }

    internal fun insertDocuments() {
        val con = dataSource.connection

        // insert i bolker av 100 dokumenter 10000 ganger
        val bulkSize = 1000
        val numBulks = 10000
        val generator = YtelsesperiodeGenerator(5000, 250000)
        println("Starter inserts")
        var totalInsertTime = 0L

        for (i in 0..numBulks) {
            val sql = "INSERT INTO spiondata(ytelsesperiode) VALUES(cast (? as json))"
            var stm = con.prepareStatement(sql)
            val ytelsesperioder = generator.take(bulkSize)

            val time = measureTimeMillis {
                ytelsesperioder.forEach{
                    stm.setString(1, mapper.writeValueAsString(it))
                    stm.addBatch()
                }
                stm.executeBatch()
            }

            totalInsertTime += time
            println("Spent $time ms inserting $bulkSize docs. Time: ")
        }

        println("Done inserting " + numBulks * bulkSize + " items in $totalInsertTime ms")
    }

    @Test
    internal fun timeQueries() {
        val con = dataSource.connection

        fun doTimedQuery(queryString : String) {
            val sw = StopWatch()
            println("Spørring: $queryString")
            sw.start()
            val results = con.prepareStatement(queryString).executeQuery()
            val virksomhetQueryTime = sw.getTime(TimeUnit.MILLISECONDS)
            var resultatListe : MutableCollection<Ytelsesperiode> = ArrayList()
            while (results.next()) {
                resultatListe.add(mapper.readValue(results.getString("ytelsesperiode")))
            }
            println("Første resultatsett: " + resultatListe.size + " treff, returnerer på $virksomhetQueryTime ms")
            val totalTimeWithLoadingAndDeseializing = sw.getTime(TimeUnit.MILLISECONDS)
            println("Laster inn alle treffene og deserialiserer dem til objekter på $totalTimeWithLoadingAndDeseializing ms")

            // Gjør samme spørring uten deserialisering

            val sw2 = StopWatch()
            sw2.start()
            val results2 = con.prepareStatement(queryString).executeQuery()
            var resultatListe2 : MutableCollection<String> = ArrayList()
            while (results2.next()) {
                resultatListe2.add(results2.getString("ytelsesperiode"))
            }
            val virksomhetQueryTime2 = sw2.getTime(TimeUnit.MILLISECONDS)
            println("Resultatsett uten serialisering: " + resultatListe2.size + " treff, returnerer på $virksomhetQueryTime2 ms")
            println("------------")
        }

        doTimedQuery("select * from spiondata where ytelsesperiode -> 'arbeidsforhold' -> 'arbeidsgiver' ->> 'virksomhetsnummer' = '111261064';")
        doTimedQuery("select  * from spiondata where ytelsesperiode -> 'arbeidsforhold' -> 'arbeidsgiver' ->> 'organisasjonsnummer' = '472157073';")
        doTimedQuery("select * from spiondata where ytelsesperiode -> 'arbeidsforhold' -> 'arbeidsgiver' ->> 'organisasjonsnummer' = '472157073' and ytelsesperiode -> 'arbeidsforhold' -> 'arbeidstaker' ->> 'identitetsnummer' = '11331346574';")
    }
}