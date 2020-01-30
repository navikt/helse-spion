package no.nav.helse.spion.auth

import no.nav.helse.spion.selfcheck.HealthCheck
import no.nav.helse.spion.selfcheck.HealthCheckType

class MockAuthRepo : AuthorizationsRepository, HealthCheck {
    override val healthCheckType = HealthCheckType.READYNESS

    private var acl: Set<String> = setOf("1")
    var failSelfCheck = false

    override fun hentRettigheterForPerson(identitetsnummer: String): Set<String> {
        return acl
    }

    fun setAccessList(acl : Set<String>) {
        this.acl = acl
    }

    override fun doHealthCheck() {
        if(failSelfCheck) throw Error("Feiler selfchecken")
    }

}