package no.nav.helse.slowtests.db.integration

import com.zaxxer.hikari.HikariDataSource
import no.nav.helse.spion.db.createLocalHikariConfig
import no.nav.helse.spion.domene.Arbeidsgiver
import no.nav.helse.spion.domene.Periode
import no.nav.helse.spion.domene.Person
import no.nav.helse.spion.domene.ytelsesperiode.Arbeidsforhold
import no.nav.helse.spion.domene.ytelsesperiode.repository.PostgresRepository
import no.nav.helse.spion.domene.ytelsesperiode.Ytelsesperiode
import no.nav.helse.spion.web.common
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.koin.core.KoinComponent
import org.koin.core.context.loadKoinModules
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import org.koin.core.get
import java.math.BigDecimal
import java.time.LocalDate
import kotlin.test.assertEquals

internal class postgresTests : KoinComponent {


    val testYtelsesPeriode = Ytelsesperiode(
            periode = Periode(LocalDate.of(2019, 1, 1), LocalDate.of(2019, 2, 1)),
            arbeidsforhold = Arbeidsforhold(
                    arbeidsforholdId = "1",
                    arbeidstaker = Person("Solan", "Gundersen", "10987654321"),
                    arbeidsgiver = Arbeidsgiver("Flåklypa Verksted", "666666666", "555555555")
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
    val dataSource = HikariDataSource(createLocalHikariConfig())

    @BeforeEach
    internal fun setUp() {
        startKoin {
            loadKoinModules(common)

        }

        val repo = PostgresRepository(dataSource, get())
        repo.save(testYtelsesPeriode)
    }

    @AfterEach
    internal fun tearDown() {
        val repo = PostgresRepository(dataSource, get())
        repo.deleteYtelsesperiode(testYtelsesPeriode)
        stopKoin()

    }

    @Test
    fun `Henter en ytelsesperiode fra repo`() {
        val repo = PostgresRepository(dataSource, get())

        val p = repo.hentYtelserForPerson("10987654321", "555555555")

        assertEquals(testYtelsesPeriode, p.first())
        assertEquals(1, p.size)
    }

    @Test
    fun `Sletter en ytelsesperiode`() {
        val repo = PostgresRepository(dataSource, get())
        val deletedCount = repo.deleteYtelsesperiode(testYtelsesPeriode)

        assertEquals(1, deletedCount)
    }

    @Test
    fun `sletter bare riktig ytelsesperiode`() {
        val repo = PostgresRepository(dataSource, get())
        val ypAnnenPeriode = testYtelsesPeriode.copy(periode = Periode(LocalDate.of(2020, 5, 5), LocalDate.of(2020, 8, 1)))
        repo.save(ypAnnenPeriode)

        val deletedCount = repo.deleteYtelsesperiode(testYtelsesPeriode)

        val ypLagret = repo.hentYtelserForPerson("10987654321", "555555555").first()

        assertEquals(1, deletedCount)
        assertEquals(ypAnnenPeriode, ypLagret)

        repo.deleteYtelsesperiode(ypAnnenPeriode)

    }

}