package no.nav.helse.spion.vedtaksmelding

import no.nav.helse.spion.domene.ytelsesperiode.Ytelsesperiode
import java.time.LocalDate

enum class VedtaksmeldingsYtelse { SP, FP, SVP, PP, OP, OM }
enum class VedtaksmeldingsStatus(val correspondingDomainStatus: Ytelsesperiode.Status) {
    BEHANDLES(Ytelsesperiode.Status.UNDER_BEHANDLING),
    INNVILGET(Ytelsesperiode.Status.INNVILGET),
    AVSLÅTT(Ytelsesperiode.Status.AVSLÅTT),
    HENLAGT(Ytelsesperiode.Status.HENLAGT)
}

data class Vedtaksmelding(
        val identitetsNummer: String,
        val virksomhetsnummer: String,
        val status: VedtaksmeldingsStatus,
        val fom: LocalDate,
        val tom: LocalDate,
        val ytelse: VedtaksmeldingsYtelse = VedtaksmeldingsYtelse.SP,

        val fornavn: String,
        val etternavn: String,

        val sykemeldingsgrad: Int?,
        val refusjonsbeloep: Double?,
        val dagsats: Double?,
        val maksDato: LocalDate?

)