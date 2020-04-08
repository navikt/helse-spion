package no.nav.helse.spion.varsling

import no.nav.helse.spion.domene.varsling.Varsling

class DummyVarslingSender(private val service: VarslingService) : VarslingSender {
    override fun
            send(varsling: Varsling) {
        println("Sender varsling med id ${varsling.uuid} til {${varsling.virksomhetsNr} med ${varsling.liste.size} personer i")
        service.oppdaterStatus(varsling, true)
    }
}