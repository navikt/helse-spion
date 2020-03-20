package no.nav.helse.spion.varsling

import no.nav.helse.spion.domene.varsling.Varsling

interface VarslingSender {
    fun send(varsling: Varsling): Boolean
}

