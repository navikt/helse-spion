package no.nav.helse.spion.auth


// --- Authorizer interface used by KTor interceptor

interface Authorizer {
    fun hasAccess(identitetsNummer : String, orgNr : String) : Boolean
}

class DefaultAuthorizer(private val authListRepo : AuthorizationsRepository) : Authorizer {
    override fun hasAccess(identitetsNummer : String, orgNr: String): Boolean {
        return authListRepo.hentRettigheterForPerson(identitetsNummer).contains(orgNr)
    }
}

// ---- Auth list repos

interface AuthorizationsRepository {
    fun hentRettigheterForPerson(identitetsNummer : String) : Set<String>
}

class MockAuthRepo : AuthorizationsRepository {
    private var acl: Set<String> = setOf("1")

    override fun hentRettigheterForPerson(identitetsNummer: String): Set<String> {
        return acl
    }

    fun setAccessList(acl : Set<String>) {
        this.acl = acl
    }
}