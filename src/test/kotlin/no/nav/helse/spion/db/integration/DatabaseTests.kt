package no.nav.helse.spion.db.integration

import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.KotlinModule
import com.fasterxml.jackson.module.kotlin.readValue
import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import io.ktor.config.ApplicationConfig
import no.nav.helse.YtelsesperiodeGenerator
import no.nav.helse.spion.db.PostgresYtelsesperiodeDao
import no.nav.helse.spion.db.YtelsesperiodeDao
import no.nav.helse.spion.db.createLocalHikariConfig
import no.nav.helse.spion.domene.Arbeidsgiver
import no.nav.helse.spion.domene.Periode
import no.nav.helse.spion.domene.Person
import no.nav.helse.spion.domene.ytelsesperiode.Arbeidsforhold
import no.nav.helse.spion.domene.ytelsesperiode.repository.PostgresRepository
import no.nav.helse.spion.domene.ytelsesperiode.Ytelsesperiode
import org.apache.commons.lang3.time.StopWatch
import org.junit.jupiter.api.Test
import java.math.BigDecimal
import java.time.LocalDate
import java.util.concurrent.TimeUnit
import kotlin.system.measureTimeMillis
import kotlin.test.assertEquals

class postgresTests {


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

    val dataSource =  HikariDataSource(createLocalHikariConfig())

    @Test
    fun `Lagrer en ytelsesperiode via repo`() {

        val repo = PostgresRepository(dataSource)
        repo.lagreYtelsesperiode(testYtelsesPeriode)
        val p = repo.hentYtelserForPerson("10987654321", "555555555")

        assertEquals(testYtelsesPeriode, p.first())

        val dao = PostgresYtelsesperiodeDao(dataSource.connection)
        dao.delete(testYtelsesPeriode.arbeidsforhold, testYtelsesPeriode.periode)

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
            var ytelsesperioder = generator.take(bulkSize)

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