package no.nav.helse.inntektsmeldingsvarsel

import java.time.LocalDate
import java.util.*

data class AltinnVarsel(
        val id: Long? = null,


        /**
         * Denne må være unik for hver melding for å kunne hente ut status på meldingen senere
         */
        val ressursId: String = UUID.randomUUID().toString(),


        val type: AltinnVarselType? = null,
        val orgnummer: String? = null,
        val aktorId: String? = null,
        val fnrSykmeldt: String? = null,
        val navnSykmeldt: String? = null,
        val soknadFom: LocalDate? = null,
        val soknadTom: LocalDate? = null
)