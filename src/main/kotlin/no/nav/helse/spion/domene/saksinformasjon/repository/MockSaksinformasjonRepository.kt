package no.nav.helse.spion.domene.saksinformasjon.repository

import no.nav.helse.spion.domene.Periode
import no.nav.helse.spion.domene.saksinformasjon.Oppsummering
import no.nav.helse.spion.domene.saksinformasjon.Saksinformasjon
import no.nav.helse.spion.domene.saksinformasjon.Sykepengeperiode
import java.math.BigDecimal
import java.time.LocalDate

class MockSaksinformasjonRepository : SaksinformasjonRepository {

    val testPeriode = Periode(LocalDate.of(2019, 1, 1), LocalDate.of(2019, 2, 1))

    override fun hentSaksinformasjon(aktørId: String): Saksinformasjon {
        return Saksinformasjon(
            oppsummering = Oppsummering(
                testPeriode,
                BigDecimal.TEN,
                LocalDate.of(2019, 1, 15)
            ),
            periodeListe = listOf(
                Sykepengeperiode(
                    testPeriode,
                    BigDecimal.TEN,
                    "GODKJENT",
                    100,
                    "SYKEPENGER"
                )
            ),
        aktørId = "AKTØRID",
        arbeidsgiver = "NAV"
        )
    }
}