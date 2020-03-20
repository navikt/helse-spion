package no.nav.helse.spion.domene.varsling.repository

import no.nav.helse.spion.domene.varsling.PersonVarsling
import no.nav.helse.spion.domene.varsling.Varsling
import java.time.LocalDate

class MockVarslingRepository(): VarslingRepository {

    private val varsling1 = Varsling(LocalDate.now(), "123", listOf(PersonVarsling("Ola Nordmann", "111222333")))
    private val varsling2 = Varsling(LocalDate.now(), "456", listOf(PersonVarsling("Kari Nordmann", "444555666")))
    private val varsling3 = Varsling(LocalDate.now(), "789", listOf(PersonVarsling("Lise Nordmann", "777888999")))

    val list = listOf(varsling1, varsling2, varsling3).toMutableList()

    override fun finnNesteUbehandlet(): Varsling {
        return list.removeAt(0)
    }

    override fun finnAntallUbehandlet(): Int {
        return list.size
    }

    override fun oppdaterStatus(varsling: Varsling, velykket: Boolean) {
        println("Oppdater $varsling med status $velykket")
    }

    override fun lagre(varsling: Varsling) {
        println("Lagret $varsling")
    }

    override fun slett(uuid: String) {
        println("Slettet $uuid")
    }

}