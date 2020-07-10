package no.nav.helse.spion.vedtaksmelding

import no.nav.helse.spion.domene.ytelsesperiode.Ytelsesperiode
import java.time.LocalDate
import java.util.*

@Deprecated(message = "Denne modellen er vår gamle 'antagelsesmodell' for melding fra spleis, og erstattes av faktisk melding")
enum class VedtaksmeldingsYtelse { SP, FP, SVP, PP, OP, OM }

@Deprecated(message = "Denne modellen er vår gamle 'antagelsesmodell' for melding fra spleis, og erstattes av faktisk melding")
enum class VedtaksmeldingsStatus(val correspondingDomainStatus: Ytelsesperiode.Status) {
    BEHANDLES(Ytelsesperiode.Status.UNDER_BEHANDLING),
    INNVILGET(Ytelsesperiode.Status.INNVILGET),
    AVSLÅTT(Ytelsesperiode.Status.AVSLÅTT),
    HENLAGT(Ytelsesperiode.Status.HENLAGT)
}

@Deprecated(message = "Denne modellen er vår gamle 'antagelsesmodell' for melding fra spleis, og erstattes av faktisk melding")
data class Vedtaksmelding(
        val identitetsnummer: String,
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

data class SpleisVedtaksperiodeDto(
        val fnr: String,
        val orgnummer: String,
        val dokumenter: List<SpleisDokument>,
        val manglendeDokumenter: List<SpleisDokument.Type>,
        val tilstand: SpleisTilstandDto
) {
    enum class SpleisTilstandDto {
        AvventerTidligerePeriode,
        AvventerDokumentasjon,
        UnderBehandling,
        AvsluttetInnenforArbeidsgiverperioden,
        Ferdigbehandlet,
        ManuellBehandling
    }
}

data class SpleisVedtakDto(
        val fom: LocalDate,
        val tom: LocalDate,
        val forbrukteSykedager: Int,
        val gjenståendeSykedager: Int,
        val utbetalinger: List<SpleisUtbetalingDto>,
        val dokumenter: List<SpleisDokument>
) {
    data class SpleisUtbetalingDto(
            val mottaker: String,
            val fagområde: String,
            val totalbeløp: Int,
            val utbetalingslinjer: List<SpleisUtbetalingslinjeDto>
    ) {
        data class SpleisUtbetalingslinjeDto(
                val fom: LocalDate,
                val tom: LocalDate,
                val dagsats: Int,
                val beløp: Int,
                val grad: Double,
                val sykedager: Int
        )
    }
}

class SpleisDokument(val dokumentId: UUID, val type: Type) {
    enum class Type {
        Sykmelding, Søknad, Inntektsmelding
    }
}

enum class SpleisMeldingstype {
    Vedtak, Behandlingstilstand
}

fun SpleisVedtakDto.snittGrad() = if (utbetalinger.isEmpty()) 0.0 else  utbetalinger.sumByDouble { it.snittGrad() } / utbetalinger.size
fun SpleisVedtakDto.SpleisUtbetalingDto.snittGrad() = if (utbetalingslinjer.isEmpty()) 0.0 else  utbetalingslinjer.sumByDouble { it.grad } / utbetalingslinjer.size

fun SpleisVedtakDto.snittDagsats() = if (utbetalinger.isEmpty()) 0 else  utbetalinger.sumBy { it.snittDagsats() } / utbetalinger.size
fun SpleisVedtakDto.SpleisUtbetalingDto.snittDagsats() = if (utbetalingslinjer.isEmpty()) 0 else  utbetalingslinjer.sumBy { it.dagsats } / utbetalingslinjer.size