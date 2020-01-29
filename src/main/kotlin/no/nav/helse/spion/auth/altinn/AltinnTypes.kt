package no.nav.helse.spion.auth.altinn

data class AltinnOrganisasjon(
    val name : String,
    val type : String,
    val parentOrganizationNumber : String?,
    val organizationForm : String?,
    val organizationNumber : String?,
    val socialSecurityNumber : String?,
    val status : String?
)