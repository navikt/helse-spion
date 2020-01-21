package no.nav.helse.spion.domene.sak

import no.nav.helse.spion.domene.Periode
import java.math.BigDecimal
import java.time.LocalDate

data class Oppsummering(
    val periode: Periode,
    val refusjonsbeløp: BigDecimal,
    val maxdato: LocalDate?
)

