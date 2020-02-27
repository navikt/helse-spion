package no.nav.helse.spion.domene.ytelsesperiode.repository

import no.nav.helse.spion.domene.Arbeidsgiver
import no.nav.helse.spion.domene.Periode
import no.nav.helse.spion.domene.Person
import no.nav.helse.spion.domene.ytelsesperiode.Arbeidsforhold
import no.nav.helse.spion.domene.ytelsesperiode.Ytelsesperiode
import java.math.BigDecimal
import java.time.LocalDate

class MockYtelsesperiodeRepository : YtelsesperiodeRepository {

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
            sistEndret = LocalDate.now(),
            kafkaOffset = 1
    )

    override fun getYtelserForPerson(identitetsnummer: String, virksomhetsnummer: String): List<Ytelsesperiode> {
        val perioder = listOf(
                Periode(LocalDate.of(2019, 1, 1), LocalDate.of(2019, 2, 1)),
                Periode(LocalDate.of(2019, 3, 10), LocalDate.of(2019, 3, 28)),
                Periode(LocalDate.of(2019, 7, 24), LocalDate.of(2019, 9, 8)),
                Periode(LocalDate.of(2019, 12, 13), LocalDate.of(2020, 1, 10)))
        val ytelsesperioder = perioder.mapIndexed { i: Int, it: Periode ->
            testYtelsesPeriode.copy(periode = it, vedtaksId = i.toString(), status = if (i % 2 == 0) Ytelsesperiode.Status.INNVILGET else Ytelsesperiode.Status.AVSLÅTT)
        }
        return ytelsesperioder
    }

    override fun upsert(yp: Ytelsesperiode) {
        println("saving $yp")
    }
}