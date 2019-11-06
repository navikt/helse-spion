package no.nav.helse.spion.domene.saksinformasjon

import no.nav.helse.spion.domene.Periode
import java.math.BigDecimal

data class Sykepengeperiode(
    val periode: Periode,
    val refusjonsbeløp: BigDecimal,
    val status: String,
    val grad: Int,
    val ytelse: String
)