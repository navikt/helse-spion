package no.nav.helse.spion.domene.sak

import no.nav.helse.spion.domene.Periode
import java.math.BigDecimal

data class Ytelsesperiode(
        val periode: Periode,
        val refusjonsbeløp: BigDecimal,
        val status: Status,
        val grad: BigDecimal,
        val ytelse: Ytelse,
        val merknad: String
) {
    enum class Status {
        INNVILGET, AVSLÅTT, UNDER_BEHANDLING
    }
    enum class Ytelse {
        SP, FP, SVP, PP, OP, OM
    }
}


