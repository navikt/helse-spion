package no.nav.helse.spion.auth

class MockAuthRepo : AuthorizationsRepository {
    private var acl: Set<String> = setOf("1")

    override fun hentRettigheterForPerson(identitetsnummer: String): Set<String> {
        return acl
    }

    fun setAccessList(acl : Set<String>) {
        this.acl = acl
    }
}