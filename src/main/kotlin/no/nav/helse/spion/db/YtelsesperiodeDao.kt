package no.nav.helse.spion.db

import no.nav.helse.spion.domene.ytelsesperiode.Ytelsesperiode

interface YtelsesperiodeDao {
    fun save(yp : Ytelsesperiode)
}

class PostgresYtelsesperiodeDao : YtelsesperiodeDao {
    override fun save(yp: Ytelsesperiode) {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }
}