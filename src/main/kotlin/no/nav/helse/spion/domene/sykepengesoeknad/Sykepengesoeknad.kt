package no.nav.helse.spion.domene.sykepengesoeknad

import no.nav.helse.spion.domene.Periode
import java.util.*

data class Sykepengesoeknad(val uuid: UUID, val periode: Periode)