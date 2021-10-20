package no.nav.helse

import no.nav.helse.spion.web.common
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.TestInstance
import org.koin.core.Koin
import org.koin.core.KoinApplication

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
abstract class KoinInjectedTest {

    internal lateinit var koin: Koin

    /*@BeforeAll
    internal fun createKoinApplicationEnvironmentWithCommonModule() {
        koin = KoinApplication.create().modules(common).koin
    }*/
}
