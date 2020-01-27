package no.nav.helse.spion.domene.sak.repository

import no.nav.helse.spion.domene.Arbeidsgiver
import no.nav.helse.spion.domene.Periode
import no.nav.helse.spion.domene.Person
import no.nav.helse.spion.domene.sak.Sak
import no.nav.helse.spion.domene.sak.Oppsummering
import no.nav.helse.spion.domene.sak.Ytelsesperiode
import java.math.BigDecimal
import java.time.LocalDate

class MockSaksinformasjonRepository : SaksinformasjonRepository {

    val testPeriode1 = Periode(LocalDate.of(2019, 1, 1), LocalDate.of(2019, 2, 1))
    val testPeriode2 = Periode(LocalDate.of(2019, 2, 12), LocalDate.of(2019, 2, 21))
    val testPeriode3 = Periode(LocalDate.of(2019, 5, 1), LocalDate.of(2019, 6, 12))
    val testOppsummeringPeriode1 = Periode(LocalDate.of(2019, 1, 1), LocalDate.of(2019, 6, 12))
    val testPeriode4 = Periode(LocalDate.of(2019, 8, 3), LocalDate.of(2019, 9, 9))
    val testPeriode5 = Periode(LocalDate.of(2019, 12, 15), LocalDate.of(2020, 1, 10))
    val testPeriode6 = Periode(LocalDate.of(2020, 1, 1), LocalDate.of(2020, 2, 1))
    val testOppsummeringPeriode2 = Periode(LocalDate.of(2019, 8, 3), LocalDate.of(2020, 2, 1))
    override fun hentArbeidsgivere(identitetsnummer: String): List<Arbeidsgiver> {
        return listOf(
                Arbeidsgiver("Etterretningstjenesten", "1965", ""),
                Arbeidsgiver("Secret Intelligence Service", "MI6", "")
        )
    }

    override fun hentSakerForPerson(identitetsnummer: String, arbeidsgiverOrgnummer: String, arbeidsgiverIdentitetsnummer: String): List<Sak> {
        return listOf(
                Sak(
                        arbeidsgiver = Arbeidsgiver(
                                navn = "CIA",
                                orgnr = "1",
                                identitetsnummer = ""),
                        person = Person(fornavn = "James", etternavn = "Bond", aktørId = "007"),
                        oppsummering = Oppsummering(
                                testOppsummeringPeriode1,
                                BigDecimal.TEN,
                                LocalDate.of(2019, 1, 15)
                        ),
                        ytelsesperioder = listOf(
                                Ytelsesperiode(
                                        periode = testPeriode1,
                                        refusjonsbeløp = BigDecimal.TEN,
                                        status = Ytelsesperiode.Status.INNVILGET,
                                        ytelse = Ytelsesperiode.Ytelse.SP,
                                        merknad = "Fritak AGP",
                                        grad = BigDecimal.ONE
                                ),
                                Ytelsesperiode(
                                        periode = testPeriode2,
                                        refusjonsbeløp = BigDecimal(300000),
                                        status = Ytelsesperiode.Status.AVSLÅTT,
                                        ytelse = Ytelsesperiode.Ytelse.SP,
                                        merknad = "",
                                        grad = BigDecimal(50)
                                ),
                                Ytelsesperiode(
                                        periode = testPeriode3,
                                        refusjonsbeløp = BigDecimal(10000),
                                        status = Ytelsesperiode.Status.INNVILGET,
                                        ytelse = Ytelsesperiode.Ytelse.SP,
                                        merknad = "",
                                        grad = BigDecimal(100)
                                )

                        )
                ),
                Sak(
                        arbeidsgiver = Arbeidsgiver(
                                navn = "CIA",
                                orgnr = "1",
                                identitetsnummer = ""),
                        person = Person(fornavn = "James", etternavn = "Bond", aktørId = "007"),
                        oppsummering = Oppsummering(
                                testOppsummeringPeriode2,
                                BigDecimal(10000),
                                LocalDate.of(2019, 1, 15)
                        ),
                        ytelsesperioder = listOf(
                                Ytelsesperiode(
                                        periode = testPeriode4,
                                        refusjonsbeløp = BigDecimal.TEN,
                                        status = Ytelsesperiode.Status.INNVILGET,
                                        ytelse = Ytelsesperiode.Ytelse.SP,
                                        merknad = "",
                                        grad = BigDecimal(100)
                                ),
                                Ytelsesperiode(
                                        periode = testPeriode5,
                                        refusjonsbeløp = BigDecimal(20000),
                                        status = Ytelsesperiode.Status.AVSLÅTT,
                                        ytelse = Ytelsesperiode.Ytelse.SP,
                                        merknad = "",
                                        grad = BigDecimal(50)
                                ),
                                Ytelsesperiode(
                                        periode = testPeriode6,
                                        refusjonsbeløp = BigDecimal(500),
                                        status = Ytelsesperiode.Status.INNVILGET,
                                        ytelse = Ytelsesperiode.Ytelse.SP,
                                        merknad = "Fritak AGP",
                                        grad = BigDecimal(25)
                                )


                        )
                )
        )
    }
}