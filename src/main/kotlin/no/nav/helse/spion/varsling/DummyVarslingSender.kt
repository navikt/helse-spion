package no.nav.helse.spion.varsling

import no.nav.helse.spion.domene.varsling.Varsling

class DummyVarslingSender : VarslingSender {

    override fun send(varsling: Varsling) {
        println("Sender varsling $varsling")
    }
}