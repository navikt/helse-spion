package no.nav.helse.spion.db.integration


import com.zaxxer.hikari.HikariDataSource
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import no.nav.helse.YtelsesperiodeGenerator
import no.nav.helse.spion.domene.ytelsesperiode.repository.PostgresYtelsesperiodeRepository
import no.nav.helse.spion.web.common
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.assertj.core.api.Assertions.assertThatExceptionOfType
import org.koin.core.KoinComponent
import org.koin.core.context.loadKoinModules
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import org.koin.core.get
import java.sql.Connection
import java.sql.ResultSet
import java.sql.SQLException

internal class dbUnitTests : KoinComponent {
    val dsMock = mockk<HikariDataSource>()
    val conMock = mockk<Connection>(relaxed = true)
    @BeforeEach
    internal fun setUp() {
        startKoin {
            loadKoinModules(common)
        }
    }

    @AfterEach
    internal fun tearDown() {
        stopKoin()
    }
    @Test
    fun `Lukker connection etter bruk`() {
        every { dsMock.connection } returns conMock

        val repo = PostgresYtelsesperiodeRepository(dsMock, get())
        repo.getYtelserForPerson("10987654321", "555555555")

        verify(exactly = 1) { dsMock.connection }
        verify(exactly = 1) { conMock.close() }
    }

    @Test
    fun `Lukker connection etter bruk selv ved feil`() {
        val rsMock = mockk<ResultSet>()

        every { dsMock.connection } returns conMock
        every { conMock.prepareStatement(any()).executeQuery() } returns rsMock
        every { rsMock.next() } returns true
        every { rsMock.getString("ytelsesperiode") } returns """ {"svaret" : 42 } """

        val repo = PostgresYtelsesperiodeRepository(dsMock, get())
        assertThatExceptionOfType(Exception::class.java).isThrownBy {
            repo.getYtelserForPerson("10987654321", "555555555")
        }

        verify(exactly = 1) { dsMock.connection }
        verify(exactly = 1) { conMock.close() }
    }

    @Test
    fun `ruller tilbake en transaksjon hvis noe feiler`() {
        val ypGen = YtelsesperiodeGenerator(maxUniqueArbeidsgivere =  10, maxUniquePersoner =  10)
        val yp = ypGen.next()

        every { dsMock.connection } returns conMock
        every {conMock.prepareStatement(any()).executeUpdate()} throws SQLException()

        val repo = PostgresYtelsesperiodeRepository(dsMock, get())

        repo.upsert(yp)

        verify(exactly = 1) {conMock.rollback()}
    }
}