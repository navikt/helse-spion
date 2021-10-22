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
