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


    override fun hentYtelserForPerson(identitetsnummer: String, orgnr: String): List<Ytelsesperiode> {
        return listOf(testYtelsesPeriode)
    }

    override fun hentArbeidsgivere(identitetsnummer: String): List<Arbeidsgiver> {
        return listOf(
                Arbeidsgiver("Etterretningstjenesten", "1965", "0", null),
                Arbeidsgiver("Secret Intelligence Service", "MI6", "1", null)
        )
    }
}