package no.nav.helse.spion.domene.sak

data class Sak(
        val oppsummering: Oppsummering,
        val ytelsesperioder: List<Ytelsesperiode>
)