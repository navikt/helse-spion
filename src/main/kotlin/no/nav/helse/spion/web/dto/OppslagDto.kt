package no.nav.helse.spion.web.dto

import no.nav.helse.spion.domene.Periode

data class OppslagDto (
    val identitetsnummer: String,
    val arbeidsgiverId: String,
    val periode: Periode? = null
)