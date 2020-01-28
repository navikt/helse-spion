package no.nav.helse.spion.auth

interface Authorizer {
    /**
     * Sjekker om den angitte identiteten har rettighet til å se refusjoner for den angitte arbeidsgiverId
     * En arbeidsgiverId kan være en virksomhet, en hovedenhet, et identitetsnummer på en privatperson eller et
     * organisasjonsledd.
     */
    fun hasAccess(identitetsnummer : String, arbeidsgiverId : String) : Boolean
}

class DefaultAuthorizer(private val authListRepo : AuthorizationsRepository) : Authorizer {
    override fun hasAccess(identitetsnummer : String, arbeidsgiverId: String): Boolean {
        return authListRepo.hentRettigheterForPerson(identitetsnummer).contains(arbeidsgiverId)
    }
}

interface AuthorizationsRepository {
    fun hentRettigheterForPerson(identitetsnummer : String) : Set<String>
}