package no.nav.helse.spion.auth

import no.nav.helse.spion.selfcheck.SelfCheck

class MockAuthRepo : AuthorizationsRepository, SelfCheck {
    private var acl: Set<String> = setOf("1")
    var failSelfCheck = false

    override fun hentRettigheterForPerson(identitetsnummer: String): Set<String> {
        return acl
    }

    fun setAccessList(acl : Set<String>) {
        this.acl = acl
    }

    override fun doSelfCheck() {
        if(failSelfCheck) throw Error("Feiler selfchecken")
    }
}