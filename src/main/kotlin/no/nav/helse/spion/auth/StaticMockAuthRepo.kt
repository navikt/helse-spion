package no.nav.helse.spion.auth

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import no.nav.helse.arbeidsgiver.kubernetes.ReadynessComponent
import no.nav.helse.spion.domene.AltinnOrganisasjon
import no.nav.helse.utils.loadFromResources

class StaticMockAuthRepo(om: ObjectMapper) : AuthorizationsRepository, ReadynessComponent {

    private var acl: Set<AltinnOrganisasjon> = setOf(AltinnOrganisasjon("Kjellesen AS", "Enterprise", null, "AS", "1", null, null))
    var failSelfCheck = false

    init {
        "mock-data/altinn/organisasjoner-med-rettighet.json".loadFromResources().let {
            acl = om.readValue<Set<AltinnOrganisasjon>>(it)
        }
    }

    override fun hentOrgMedRettigheterForPerson(identitetsnummer: String): Set<AltinnOrganisasjon> {
        return acl
    }

    fun setAccessList(acl: Set<AltinnOrganisasjon>) {
        this.acl = acl
    }

    override suspend fun runReadynessCheck() {
        if (failSelfCheck) throw Error("Feiler selfchecken")
    }
}
