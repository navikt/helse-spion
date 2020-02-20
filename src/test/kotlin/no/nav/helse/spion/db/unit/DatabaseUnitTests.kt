package no.nav.helse.spion.db.integration


import com.zaxxer.hikari.HikariDataSource
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
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
import org.junit.jupiter.api.assertThrows
import org.koin.core.KoinComponent
import org.koin.core.context.loadKoinModules
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import org.koin.core.get
import java.lang.Exception
import java.math.BigDecimal
import java.sql.Connection
import java.sql.ResultSet
import java.time.LocalDate

internal class dbUnitTests : KoinComponent {


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
    val dataSource = HikariDataSource(createLocalHikariConfig())

    @BeforeEach
    internal fun setUp() {
        startKoin {
            loadKoinModules(common)

        }

        val repo = PostgresRepository(dataSource, get())
        repo.saveYtelsesperiode(testYtelsesPeriode)
    }

    @AfterEach
    internal fun tearDown() {
        val repo = PostgresRepository(dataSource, get())
        repo.deleteYtelsesperiode(testYtelsesPeriode)
        stopKoin()

    }
    @Test
    fun `Lukker connection etter bruk`() {
        val dsMock = mockk<HikariDataSource>()
        val conMock = mockk<Connection>(relaxed = true)

        every { dsMock.connection } returns conMock

        val repo = PostgresRepository(dsMock, get())
        repo.hentYtelserForPerson("10987654321", "555555555")

        verify(exactly = 1) { dsMock.connection }
        verify(exactly = 1) { conMock.close() }
    }

    @Test
    fun `Lukker connection etter bruk selv ved feil`() {
        val dsMock = mockk<HikariDataSource>()
        val conMock = mockk<Connection>(relaxed = true)
        val rsMock = mockk<ResultSet>()

        every { dsMock.connection } returns conMock
        every { conMock.prepareStatement(any()).executeQuery() } returns rsMock
        every { rsMock.next() } returns true
        every { rsMock.getString("ytelsesperiode") } returns """ {"svaret" : 42 } """

        val repo = PostgresRepository(dsMock, get())
        assertThrows<Exception> {
            repo.hentYtelserForPerson("10987654321", "555555555")
        }
        verify(exactly = 1) { dsMock.connection }
        verify(exactly = 1) { conMock.close() }
    }

}