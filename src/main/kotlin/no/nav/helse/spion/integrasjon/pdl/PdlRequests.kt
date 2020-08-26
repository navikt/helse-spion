package no.nav.helse.spion.integrasjon.pdl

data class PdlRequest(
        val query: String,
        val variables: Variables
)

data class Variables(
        val ident: String,
        val navnHistorikk: Boolean = false
)