package no.nav.helse.slowtests.db.integration

import com.fasterxml.jackson.databind.ObjectMapper
import com.zaxxer.hikari.HikariDataSource
import io.mockk.every
import io.mockk.mockk
import no.nav.helse.spion.db.createLocalHikariConfig
import no.nav.helse.spion.domene.Arbeidsgiver
import no.nav.helse.spion.domene.Periode
import no.nav.helse.spion.domene.Person
import no.nav.helse.spion.domene.ytelsesperiode.Arbeidsforhold
import no.nav.helse.spion.domene.ytelsesperiode.Ytelsesperiode
import no.nav.helse.spion.domene.ytelsesperiode.repository.PostgresYtelsesperiodeRepository
import no.nav.helse.spion.web.common
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatExceptionOfType
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.koin.core.component.KoinComponent
import org.koin.core.component.get
import org.koin.core.context.loadKoinModules
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import org.postgresql.util.PSQLException
import java.math.BigDecimal
import java.time.LocalDate

internal class PostgresYtelsesperiodeRepositoryTest : KoinComponent {

    lateinit var repo: PostgresYtelsesperiodeRepository
    val testYtelsesPeriode = Ytelsesperiode(
        periode = Periode(LocalDate.of(2019, 1, 1), LocalDate.of(2019, 2, 1)),
        kafkaOffset = 2,
        forbrukteSykedager = 2,
        gjenståendeSykedager = 5,
        arbeidsforhold = Arbeidsforhold(
            arbeidsforholdId = "1",
            arbeidstaker = Person("Solan", "Gundersen", "10987654321"),
            arbeidsgiver = Arbeidsgiver("555555555")
        ),
        refusjonsbeløp = BigDecimal(10000),
        status = Ytelsesperiode.Status.UNDER_BEHANDLING,
        grad = BigDecimal(50),
        dagsats = BigDecimal(200),
        ytelse = Ytelsesperiode.Ytelse.SP,
        sistEndret = LocalDate.now()
    )

    @BeforeEach
    internal fun setUp() {
        startKoin {
            loadKoinModules(common)
        }
        repo = PostgresYtelsesperiodeRepository(HikariDataSource(createLocalHikariConfig()), get())
        repo.upsert(testYtelsesPeriode)
    }

    @AfterEach
    internal fun tearDown() {
        repo.delete(testYtelsesPeriode)
        stopKoin()
    }

    @Test
    fun `Henter en ytelsesperiode fra repo`() {
        val yp = repo.getYtelserForPerson("10987654321", "555555555")

        assertThat(yp.size).isEqualTo(1)
        assertThat(yp.first()).isEqualTo(testYtelsesPeriode)
    }

    @Test
    fun `Henter ytelsesperiode med virksomhetsnummer og periode`() {
        val yp = repo.getYtelserForVirksomhet("555555555", testYtelsesPeriode.periode)

        assertThat(yp.size).isEqualTo(1)
        assertThat(yp.first()).isEqualTo(testYtelsesPeriode)
    }

    @Test
    fun `Sletter en ytelsesperiode`() {
        val deletedCount = repo.delete(testYtelsesPeriode)

        assertThat(deletedCount).isEqualTo(1)
    }

    @Test
    fun `sletter bare riktig ytelsesperiode`() {
        val ypAnnenPeriode = testYtelsesPeriode.copy(periode = Periode(LocalDate.of(2020, 5, 5), LocalDate.of(2020, 8, 1)))
        repo.upsert(ypAnnenPeriode)

        val deletedCount = repo.delete(testYtelsesPeriode)

        val ypLagret = repo.getYtelserForPerson("10987654321", "555555555")

        assertThat(deletedCount).isEqualTo(1)
        assertThat(ypLagret).containsOnly(ypAnnenPeriode)

        repo.delete(ypAnnenPeriode)
    }

    @Test
    fun `lagrer en nyere ytelsesperiode`() {
        val ypNewer = testYtelsesPeriode.copy(kafkaOffset = 5, status = Ytelsesperiode.Status.INNVILGET)

        repo.upsert(ypNewer)

        val savedYpList = repo.getYtelserForPerson("10987654321", "555555555")

        assertThat(savedYpList).containsOnly(ypNewer)
    }

    @Test
    fun `lagrer ytelsesperiode kun hvis den har høyere offset enn en eksisterende versjon`() {
        val ypNewer = testYtelsesPeriode.copy(kafkaOffset = 3, status = Ytelsesperiode.Status.INNVILGET)
        val ypOlder = testYtelsesPeriode.copy(kafkaOffset = 1, status = Ytelsesperiode.Status.HENLAGT)

        repo.upsert(ypNewer)
        repo.upsert(ypOlder)

        val savedYpList = repo.getYtelserForPerson("10987654321", "555555555")

        assertThat(savedYpList).containsOnly(ypNewer)
    }

    @Test
    fun `lagrer ikke to ytelsesperioder med samme primærnøkkel`() {
        val con = HikariDataSource(createLocalHikariConfig()).connection

        val yp = testYtelsesPeriode.copy(kafkaOffset = 3, status = Ytelsesperiode.Status.INNVILGET)

        assertThatExceptionOfType(PSQLException::class.java).isThrownBy {
            repo.executeSave(yp, con)
        }
    }

    @Test
    fun `lagrer ikke en ytelsesperiode som mangler del av primærnøkkel`() {
        val ds = HikariDataSource(createLocalHikariConfig())
        val mapperMock = mockk<ObjectMapper>()
        val validJsonMissingIdentitetsnummer = "{  \"periode\" : {    \"fom\" : \"2019-01-01\",    \"tom\" : \"2019-02-01\"  },  \"kafkaOffset\" : 2,  \"arbeidsforhold\" : {    \"arbeidsforholdId\" : \"1\",    \"arbeidstaker\" : {      \"fornavn\" : \"Solan\",      \"etternavn\" : \"Gundersen\"   },    \"arbeidsgiver\" : {      \"navn\" : \"Flåklypa Verksted\",      \"organisasjonsnummer\" : \"666666666\",      \"arbeidsgiverId\" : \"555555555\"    }  },  \"vedtaksId\" : \"1\",  \"refusjonsbeløp\" : 10000,  \"status\" : \"UNDER_BEHANDLING\",  \"grad\" : 50,  \"dagsats\" : 200,  \"maxdato\" : \"2019-01-01\",  \"ferieperioder\" : [ ],  \"ytelse\" : \"SP\",  \"merknad\" : \"Fritak fra AGP\",  \"sistEndret\" : \"2020-02-26\"}"
        every { mapperMock.writeValueAsString(any()) } returns validJsonMissingIdentitetsnummer

        repo = PostgresYtelsesperiodeRepository(ds, mapperMock)

        assertThatExceptionOfType(PSQLException::class.java).isThrownBy {
            repo.executeSave(testYtelsesPeriode, ds.connection)
        }
    }

    @Test
    fun `henter perioder innenfor gitt tidsperiode`() {
        val withinRange = testYtelsesPeriode.copy(periode = Periode(fom = LocalDate.of(2022, 12, 20), tom = LocalDate.of(2022, 12, 30)))
        val fomWithinRange = testYtelsesPeriode.copy(periode = Periode(fom = LocalDate.of(2023, 1, 1), tom = LocalDate.of(2023, 2, 1)))
        val tomWithinRange = testYtelsesPeriode.copy(periode = Periode(LocalDate.of(2022, 10, 1), LocalDate.of(2022, 12, 30)))
        val ypBefore = testYtelsesPeriode.copy(periode = Periode(LocalDate.of(2022, 2, 3), LocalDate.of(2022, 3, 1)))
        val ypAfter = testYtelsesPeriode.copy(periode = Periode(LocalDate.of(2023, 2, 1), LocalDate.of(2023, 3, 1)))
        val yps = listOf(withinRange, fomWithinRange, tomWithinRange, ypBefore, ypAfter)
        val queryRange = Periode(fom = LocalDate.of(2022, 12, 20), tom = LocalDate.of(2023, 1, 10))

        yps.forEach {
            repo.upsert(it)
        }

        val result = repo.getYtelserForPerson(testYtelsesPeriode.arbeidsforhold.arbeidstaker.identitetsnummer, testYtelsesPeriode.arbeidsforhold.arbeidsgiver.arbeidsgiverId, queryRange)

        assertThat(result).hasSize(3)
            .containsOnly(withinRange, fomWithinRange, tomWithinRange)

        yps.forEach {
            repo.delete(it)
        }
    }
}
