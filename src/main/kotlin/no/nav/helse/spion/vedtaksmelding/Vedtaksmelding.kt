package no.nav.helse.spion.vedtaksmelding

import java.time.LocalDate
import java.util.UUID

/* Behandling
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
*/

data class SpleisUtbetalingDto(
    val event: UtbetalingEventType,
    val utbetalingId: String,
    val foedselsnummer: String,
    val aktørid: Int,
    val fom: LocalDate,
    val tom: LocalDate,
    val forbrukteSykedager: Int,
    val gjenståendeSykedager: Int,
    val automatiskBehandling: Boolean,
    val arbeidsgiverOppdrag: List<ArbeidsgiverOppdragDTO>,
    val type: UtbetalingType,
    val utbetalingsdager: List<UtbetalingsdagerDTO>
) {
    data class ArbeidsgiverOppdragDTO(
        val mottaker: String,
        val fagområde: String,
        val fagsystemId: String,
        val nettoBeløp: Int,
        val utbetalingslinjer: List<UtbetalingUtbetalingslinjer>
    ) {
        data class UtbetalingUtbetalingslinjer(
            val fom: LocalDate,
            val tom: LocalDate,
            val dagsats: Int,
            val totalbeløp: Int,
            val grad: Double,
            val stønadsdager: Int
        )
    }
}

data class UtbetalingsdagerDTO(
    val felt: String,
    val dato: LocalDate,
    val type: Forklaring,
    val begrunnelse: String
)

enum class Forklaring {
    SykepengedagerOppbrukt,
    SykepengedagerOppbruktOver67,
    MinimumInntekt,
    MinimumInntektOver67,
    EgenmeldingUtenforArbeidsgiverperiode,
    MinimumSykdomsgrad,
    EtterDødsdato,
    ManglerOpptjening,
    ManglerMedlemskap,
    Over70
}

enum class UtbetalingType {
    UTBETALING, ETTERUTBETALING, ANNULERING, REVURDERING
}

enum class UtbetalingEventType {
    utbetaling_utbetalt, utbetaling_uten_utbetaling
}

// data class SpleisVedtakDto(
//    val fom: LocalDate,
//    val tom: LocalDate,
//    val forbrukteSykedager: Int,
//    val gjenståendeSykedager: Int,
//    val utbetalinger: List<SpleisUtbetalingDto>,
//    val dokumenter: List<SpleisDokument>
// ) {
//    data class SpleisUtbetalingDto(
//        val mottaker: String,
//        val fagområde: String,
//        val totalbeløp: Int,
//        val utbetalingslinjer: List<SpleisUtbetalingslinjeDto>
//    ) {
//        data class SpleisUtbetalingslinjeDto(
//            val fom: LocalDate,
//            val tom: LocalDate,
//            val dagsats: Int,
//            val beløp: Int,
//            val grad: Double,
//            val sykedager: Int
//        )
//    }
// }

class SpleisDokument(val dokumentId: UUID, val type: Type) {
    enum class Type {
        Sykmelding, Søknad, Inntektsmelding
    }
}

enum class SpleisMeldingstype {
    Vedtak, Behandlingstilstand
}
