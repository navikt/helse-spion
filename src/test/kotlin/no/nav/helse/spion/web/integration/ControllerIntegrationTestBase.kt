package no.nav.helse.spion.web.integration

import io.ktor.application.Application
import io.ktor.config.ApplicationConfig
import io.ktor.config.MapApplicationConfig
import io.ktor.http.HttpMethod
import io.ktor.server.testing.TestApplicationCall
import io.ktor.server.testing.TestApplicationEngine
import io.ktor.server.testing.TestApplicationRequest
import io.ktor.server.testing.createTestEnvironment
import io.ktor.server.testing.withApplication
import no.nav.helse.TestData
import no.nav.helse.spion.web.selectModuleBasedOnProfile
import no.nav.security.mock.oauth2.MockOAuth2Server
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.TestInstance
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import org.koin.test.KoinTest

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
open class ControllerIntegrationTestBase : KoinTest {

    protected val defaultSubject = TestData.validIdentitetsnummer
    private val testConfig: ApplicationConfig
    protected val idTokenCookieName = "selvbetjening-idtoken"
    var server: MockOAuth2Server? = null

    @BeforeAll
    fun before() {
        server = MockOAuth2Server()
        server!!.start()
        startKoin { modules(selectModuleBasedOnProfile(testConfig)) }
    }

    @AfterAll
    fun after() {
        server!!.shutdown()
        stopKoin()
    }

    init {
        testConfig = MapApplicationConfig()
        addIntegrationTestConfigValues(testConfig)
    }

    fun <R> configuredTestApplication(moduleFunction: Application.() -> Unit, test: TestApplicationEngine.() -> R): R {
        return withApplication(createTestEnvironment()) {
            addIntegrationTestConfigValues(application.environment.config as MapApplicationConfig)
            moduleFunction(application)
            test()
        }
    }

    fun TestApplicationEngine.doAuthenticatedRequest(
        method: HttpMethod,
        uri: String,
        setup: TestApplicationRequest.() -> Unit = {}
    ): TestApplicationCall = handleRequest {

        this.uri = uri
        this.method = method
        addHeader("Authorization", "Bearer ${server?.issueToken()?.serialize()}")
        setup()
    }

    private fun addIntegrationTestConfigValues(config: MapApplicationConfig) {
        val acceptedIssuer = "default"
        val acceptedAudience = "default"

        config.apply {
            put("koin.profile", "TEST")
            put("no.nav.security.jwt.issuers.size", "1")
            put("no.nav.security.jwt.issuers.0.issuer_name", acceptedIssuer)
            put("no.nav.security.jwt.issuers.0.discoveryurl", server?.wellKnownUrl(acceptedIssuer).toString())
            put("no.nav.security.jwt.issuers.0.accepted_audience", acceptedAudience)
            put("no.nav.security.jwt.issuers.0.cookie_name", idTokenCookieName)
        }
    }
}
