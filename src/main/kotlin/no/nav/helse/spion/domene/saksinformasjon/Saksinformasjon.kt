package no.nav.helse.spion.domene.saksinformasjon

data class Saksinformasjon(
    val oppsummering: Oppsummering,
    val periodeListe: List<Sykepengeperiode>
)