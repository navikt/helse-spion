package no.nav.helse.slowtests.db.integration

import com.zaxxer.hikari.HikariDataSource
import no.nav.helse.spion.db.createLocalHikariConfig
import no.nav.helse.spion.domene.Arbeidsgiver
import no.nav.helse.spion.domene.Periode
import no.nav.helse.spion.domene.Person
import no.nav.helse.spion.domene.ytelsesperiode.Arbeidsforhold
import no.nav.helse.spion.domene.ytelsesperiode.repository.PostgresYtelsesperiodeRepository
import no.nav.helse.spion.domene.ytelsesperiode.Ytelsesperiode
import no.nav.helse.spion.web.common
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.koin.core.KoinComponent
import org.koin.core.context.loadKoinModules
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import org.koin.core.get
import org.postgresql.util.PSQLException
import java.lang.Exception
import java.math.BigDecimal
import java.time.LocalDate
import kotlin.test.assertEquals

internal class postgresYtelsesperiodeRepositoryTest : KoinComponent {


    val testYtelsesPeriode = Ytelsesperiode(
            periode = Periode(LocalDate.of(2019, 1, 1), LocalDate.of(2019, 2, 1)),
            kafkaOffset = 2,
            arbeidsforhold = Arbeidsforhold(
                    arbeidsforholdId = "1",
                    arbeidstaker = Person("Solan", "Gundersen", "10987654321"),
                    arbeidsgiver = Arbeidsgiver("Flåklypa Verksted", "666666666", "555555555")
            ),
            vedtaksId = "1",
            refusjonsbeløp = BigDecimal(10000),
            status = Ytelsesperiode.Status.UNDER_BEHANDLING,
            grad = BigDecimal(50),
            dagsats = BigDecimal(200),
            maxdato = LocalDate.of(2019, 1, 1),
            ferieperioder = emptyList(),
            ytelse = Ytelsesperiode.Ytelse.SP,
            merknad = "Fritak fra AGP",
            sistEndret = LocalDate.now()
    )
    val dataSource = HikariDataSource(createLocalHikariConfig())

    @BeforeEach
    internal fun setUp() {
        startKoin {
            loadKoinModules(common)

        }

        val repo = PostgresYtelsesperiodeRepository(dataSource, get())
        repo.upsert(testYtelsesPeriode)
    }

    @AfterEach
    internal fun tearDown() {
        val repo = PostgresYtelsesperiodeRepository(dataSource, get())
        repo.delete(testYtelsesPeriode)
        stopKoin()

    }

    @Test
    fun `Henter en ytelsesperiode fra repo`() {
        val repo = PostgresYtelsesperiodeRepository(dataSource, get())

        val p = repo.getYtelserForPerson("10987654321", "555555555")

        assertEquals(testYtelsesPeriode, p.first())
        assertEquals(1, p.size)
    }

    @Test
    fun `Sletter en ytelsesperiode`() {
        val repo = PostgresYtelsesperiodeRepository(dataSource, get())
        val deletedCount = repo.delete(testYtelsesPeriode)

        assertEquals(1, deletedCount)
    }

    @Test
    fun `sletter bare riktig ytelsesperiode`() {
        val repo = PostgresYtelsesperiodeRepository(dataSource, get())
        val ypAnnenPeriode = testYtelsesPeriode.copy(periode = Periode(LocalDate.of(2020, 5, 5), LocalDate.of(2020, 8, 1)))
        repo.upsert(ypAnnenPeriode)

        val deletedCount = repo.delete(testYtelsesPeriode)

        val ypLagret = repo.getYtelserForPerson("10987654321", "555555555").first()

        assertEquals(1, deletedCount)
        assertEquals(ypAnnenPeriode, ypLagret)

        repo.delete(ypAnnenPeriode)

    }

    @Test
    fun `lagrer en nyere ytelsesperiode`() {
        val repo = PostgresYtelsesperiodeRepository(dataSource, get())
        val ypNewer = testYtelsesPeriode.copy(kafkaOffset = 5, status = Ytelsesperiode.Status.INNVILGET)

        repo.upsert(ypNewer)

        val savedYpList = repo.getYtelserForPerson("10987654321", "555555555")

        assertEquals(savedYpList.size, 1)
        assertEquals(savedYpList.first(), ypNewer)

    }

    @Test
    fun `lagrer ytelsesperiode kun hvis den har høyere offset enn en eksisterende versjon`() {
        val repo = PostgresYtelsesperiodeRepository(dataSource, get())
        val ypNewer = testYtelsesPeriode.copy(kafkaOffset = 3, status = Ytelsesperiode.Status.INNVILGET)
        val ypOlder = testYtelsesPeriode.copy(kafkaOffset = 1, status = Ytelsesperiode.Status.HENLAGT)

        repo.upsert(ypNewer)
        repo.upsert(ypOlder)

        val savedYpList = repo.getYtelserForPerson("10987654321", "555555555")

        assertEquals(1, savedYpList.size)
        assertEquals(ypNewer, savedYpList.first())
    }

    @Test
    fun `lagrer aldri to ytelsesperioder med samme primærnøkkel`() {
        val repo = PostgresYtelsesperiodeRepository(dataSource, get())

        val yp = testYtelsesPeriode.copy(kafkaOffset = 3, status = Ytelsesperiode.Status.INNVILGET)

        assertThrows<PSQLException> {
            repo.executeSave(yp, dataSource.connection)
        }
    }
}