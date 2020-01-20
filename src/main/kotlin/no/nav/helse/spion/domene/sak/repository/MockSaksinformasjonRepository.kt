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

    val testPeriode = Periode(LocalDate.of(2019, 1, 1), LocalDate.of(2019, 2, 1))

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
                                testPeriode,
                                BigDecimal.TEN,
                                LocalDate.of(2019, 1, 15)
                        ),
                        ytelsesperioder = listOf(
                                Ytelsesperiode(
                                        periode = testPeriode,
                                        refusjonsbeløp = BigDecimal.TEN,
                                        status = Ytelsesperiode.Status.INNVILGET,
                                        ytelse = Ytelsesperiode.Ytelse.SP,
                                        merknad = "Fritak AGP",
                                        grad = BigDecimal.ONE
                                )
                        )
                )
        )
    }
}