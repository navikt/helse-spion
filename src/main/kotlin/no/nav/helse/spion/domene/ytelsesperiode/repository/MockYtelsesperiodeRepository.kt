package no.nav.helse.spion.domene.ytelsesperiode.repository

import no.nav.helse.spion.domene.Arbeidsgiver
import no.nav.helse.spion.domene.Periode
import no.nav.helse.spion.domene.Person
import no.nav.helse.spion.domene.ytelsesperiode.Arbeidsforhold
import no.nav.helse.spion.domene.ytelsesperiode.Ytelsesperiode
import java.math.BigDecimal
import java.time.LocalDate

class MockYtelsesperiodeRepository : YtelsesperiodeRepository {

    private val ytelsesperioder: List<Ytelsesperiode>

    val testYtelsesPeriode = Ytelsesperiode(
            periode = Periode(LocalDate.of(2019, 1, 1), LocalDate.of(2019, 2, 1)),
            gjenståendeSykedager = 2,
            forbrukteSykedager = 82,
            arbeidsforhold = Arbeidsforhold(
                    arbeidsforholdId = "1",
                    arbeidstaker = Person("Solan", "Gundersen", "10987654321"),
                    arbeidsgiver = Arbeidsgiver("555555555")
            ),
            refusjonsbeløp = BigDecimal(10000),
            status = Ytelsesperiode.Status.INNVILGET,
            grad = BigDecimal(50),
            dagsats = BigDecimal(200),
            sistEndret = LocalDate.now(),
            ytelse = Ytelsesperiode.Ytelse.SP,
            kafkaOffset = 1
    )

    init {
        val perioder = listOf(
                Periode(LocalDate.of(2019, 1, 1), LocalDate.of(2019, 2, 1)),
                Periode(LocalDate.of(2019, 3, 10), LocalDate.of(2019, 3, 28)),
                Periode(LocalDate.of(2019, 7, 24), LocalDate.of(2019, 9, 8)),
                Periode(LocalDate.of(2019, 12, 13), LocalDate.of(2020, 1, 10)))
        ytelsesperioder = perioder.mapIndexed { i: Int, it: Periode ->
            testYtelsesPeriode.copy(periode = it, status = if (i % 2 == 0) Ytelsesperiode.Status.INNVILGET else Ytelsesperiode.Status.AVSLÅTT)
        }
    }

    override fun getYtelserForPerson(identitetsnummer: String, virksomhetsnummer: String, periode: Periode?): List<Ytelsesperiode> {
        return ytelsesperioder
    }

    override fun getYtelserForVirksomhet(virksomhetsnummer: String, periode: Periode): List<Ytelsesperiode> {
        return ytelsesperioder
    }

    override fun upsert(yp: Ytelsesperiode) {
        println("saving $yp")
    }
}
