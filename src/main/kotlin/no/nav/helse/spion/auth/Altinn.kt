package no.nav.helse.spion.auth


// --- Authorizer interface used by KTor interceptor

interface Authorizer {
    fun hasAccess(identitetsnummer : String, orgNr : String) : Boolean
}

class DefaultAuthorizer(private val authListRepo : AuthorizationsRepository) : Authorizer {
    override fun hasAccess(identitetsnummer : String, orgNr: String): Boolean {
        return authListRepo.hentRettigheterForPerson(identitetsnummer).contains(orgNr)
    }
}

// ---- Auth list repos

interface AuthorizationsRepository {
    fun hentRettigheterForPerson(identitetsnummer : String) : Set<String>
}

class AltinnClient : AuthorizationsRepository {
    override fun hentRettigheterForPerson(identitetsnummer: String): Set<String> {
        return setOf("987654321")
    }
}

class MockAuthRepo : AuthorizationsRepository {
    private var acl: Set<String> = setOf("1")

    override fun hentRettigheterForPerson(identitetsnummer: String): Set<String> {
        return acl
    }

    fun setAccessList(acl : Set<String>) {
        this.acl = acl
    }
}