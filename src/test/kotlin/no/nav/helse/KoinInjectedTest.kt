package no.nav.helse

import org.junit.jupiter.api.TestInstance
import org.koin.core.Koin

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
abstract class KoinInjectedTest {

    internal lateinit var koin: Koin

    /*@BeforeAll
    internal fun createKoinApplicationEnvironmentWithCommonModule() {
        koin = KoinApplication.create().modules(common).koin
    }*/
}
