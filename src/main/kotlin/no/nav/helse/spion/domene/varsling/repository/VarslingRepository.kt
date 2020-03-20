package no.nav.helse.spion.domene.varsling.repository

import no.nav.helse.spion.domene.varsling.Varsling

interface VarslingRepository {
    fun finnNesteUbehandlet() : Varsling
    fun finnAntallUbehandlet() : Int
    fun oppdaterStatus(varsling: Varsling, velykket: Boolean)
    fun lagre(varsling: Varsling)
    fun slett(uuid: String)
}