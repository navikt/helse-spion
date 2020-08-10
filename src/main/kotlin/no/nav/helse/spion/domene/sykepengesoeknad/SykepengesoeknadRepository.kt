package no.nav.helse.spion.domene.sykepengesoeknad

import java.util.*

interface SykepengesoeknadRepository {
    fun getById(id: UUID): Sykepengesoeknad?
    fun upsert(entity: Sykepengesoeknad): Int
}