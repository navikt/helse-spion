package no.nav.helse.spion.domene.sak.repository

import no.nav.helse.spion.domene.Periode
import no.nav.helse.spion.domene.sak.Arbeidsgiver
import no.nav.helse.spion.domene.sak.Sak
import no.nav.helse.spion.domene.sak.Oppsummering
import no.nav.helse.spion.domene.sak.Person
import no.nav.helse.spion.domene.sak.Status
import no.nav.helse.spion.domene.sak.Ytelse
import no.nav.helse.spion.domene.sak.Ytelsesperiode
import java.math.BigDecimal
import java.time.LocalDate

class MockSaksinformasjonRepository : SaksinformasjonRepository {

    val testPeriode = Periode(LocalDate.of(2019, 1, 1), LocalDate.of(2019, 2, 1))

    override fun hentSakerForPerson(token: String, pnr: String): Sak {
        return Sak(
                oppsummering = Oppsummering(
                        testPeriode,
                        BigDecimal.TEN,
                        LocalDate.of(2019, 1, 15)
                ),
                arbeidsgiver = Arbeidsgiver(
                        navn = "CIA",
                        orgnr = "1",
                        personnummer = ""
                ),
                person = Person(fornavn = "James", etternavn = "Bond"),
                ytelsesperioder = listOf(
                        Ytelsesperiode(
                                periode = testPeriode,
                                refusjonsbeløp = BigDecimal.TEN,
                                status = Status.INNVILGET,
                                ytelse = Ytelse.SP,
                                merknad = "Fritak AGP",
                                grad = BigDecimal.ONE
                        )
                )

        )
    }
}