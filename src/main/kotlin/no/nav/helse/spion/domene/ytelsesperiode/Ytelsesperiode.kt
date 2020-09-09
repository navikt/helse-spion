package no.nav.helse.spion.domene.ytelsesperiode

import no.nav.helse.spion.domene.Periode
import java.math.BigDecimal
import java.time.LocalDate

data class Ytelsesperiode(
        val periode: Periode,
        val kafkaOffset: Long,
        val forbrukteSykedager: Int,
        val gjenståendeSykedager: Int,
        val arbeidsforhold: Arbeidsforhold,
        val refusjonsbeløp: BigDecimal?,
        val status: Status,
        val grad: BigDecimal?,
        val dagsats: BigDecimal?,
        val ytelse: Ytelse,
        val sistEndret: LocalDate
) {
    enum class Status {
        INNVILGET, AVSLÅTT, UNDER_BEHANDLING, HENLAGT
    }

    enum class Ytelse {
        SP, FP, SVP, PP, OP, OM
    }
}
