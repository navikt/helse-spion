package no.nav.helse.spion.varsling

import no.nav.helse.spion.domene.varsling.Varsling
import no.nav.helse.spion.domene.varsling.repository.VarslingRepository

class VarslingQueue(val repository: VarslingRepository) {

    fun check(sender: VarslingSender){
        while (!isEmpty()) {
            getNext()?.let { v -> updateVarsling(v, sender.send(v)) }
        }
    }

    fun isEmpty() : Boolean {
        return repository.finnAntallUbehandlet() == 0
    }

    fun getNext() : Varsling? {
        return repository.finnNesteUbehandlet()
    }

    fun updateVarsling(varsling: Varsling, velykket: Boolean){
        return repository.oppdaterStatus(varsling, velykket)
    }

}
