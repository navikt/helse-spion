package no.nav.helse.spion.integrasjon.pdl

data class PdlPersonResponse(
        val errors: List<PdlError>?,
        val data: PdlHentPerson?
)

data class PdlError(
        val message: String,
        val extensions: PdlErrorExtension
)

data class PdlErrorExtension(
        val code: String?,
        val classification: String
)

data class PdlHentPerson(
        val hentPerson: PdlPerson?
)

data class PdlPerson(
        val navn: List<PdlPersonNavn>,
        val adressebeskyttelse: List<Adressebeskyttelse>?
)

data class PdlPersonNavn(
        val fornavn: String,
        val mellomnavn: String?,
        val etternavn: String
)

data class Adressebeskyttelse(
        val gradering: Gradering
)

enum class Gradering {
    STRENGT_FORTROLIG,
    FORTROLIG,
    UGRADERT
}

fun PdlError.errorMessage(): String {
    return "${this.message} with code: ${extensions.code} and classification: ${extensions.classification}"
}