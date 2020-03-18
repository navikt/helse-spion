package no.nav.helse.inntektsmeldingsvarsel

import java.time.LocalDate

data class AltinnVarsel(
        val id: Long? = null,
        val ressursId: String? = null,
        val type: AltinnVarselType? = null,
        val orgnummer: String? = null,
        val aktorId: String? = null,
        val fnrSykmeldt: String? = null,
        val navnSykmeldt: String? = null,
        val soknadFom: LocalDate? = null,
        val soknadTom: LocalDate? = null
)