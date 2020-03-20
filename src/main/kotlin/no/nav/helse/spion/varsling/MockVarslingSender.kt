package no.nav.helse.spion.varsling

import no.nav.helse.spion.domene.varsling.Varsling

class MockVarslingSender : VarslingSender {

    override fun send(varsling: Varsling) : Boolean {
        println("Sender varsling $varsling")
        return true
    }

}