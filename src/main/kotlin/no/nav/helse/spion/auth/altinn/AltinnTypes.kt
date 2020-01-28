package no.nav.helse.spion.auth.altinn

import com.fasterxml.jackson.annotation.JsonProperty

data class AltinnOrganisasjon(
        @JsonProperty("Name") val name : String,
        @JsonProperty("Type") val type : String,
        @JsonProperty("ParentOrganizationNumber") val parentOrganizationNumber : String?,
        @JsonProperty("OrganizationForm") val organizationForm : String?,
        @JsonProperty("OrganizationNumber") val organizationNumber : String?,
        @JsonProperty("SocialSecurityNumber") val socialSecurityNumber : String?,
        @JsonProperty("Status") val status : String?
)