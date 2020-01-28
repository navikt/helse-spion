package no.nav.helse.spion.auth.altinn

data class AltinnOrganisasjon(
        val name : String,
        val type : String,
        val parentOrganizationNumber : String,
        val organizationNumber : String,
        val organizationForm : String,
        val status : String
)