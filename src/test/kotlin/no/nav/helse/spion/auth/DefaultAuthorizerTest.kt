package no.nav.helse.spion.auth

import io.mockk.every
import io.mockk.mockk
import no.nav.helse.spion.domene.AltinnOrganisasjon
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

internal class DefaultAuthorizerTest {
    lateinit var authorizer : DefaultAuthorizer

    private val subjectWithAccess = "02038509876"
    private val doesNotHaveAccess = "xxxxxxxxxx"

    @BeforeEach
    internal fun setUp() {
        val authRepoMock = mockk<AuthorizationsRepository>()
        every { authRepoMock.hentOrgMedRettigheterForPerson(subjectWithAccess) } returns setOf(
                AltinnOrganisasjon("test", "Enterprise", organizationNumber = "123"),
                AltinnOrganisasjon("test 2", "Enterprise", organizationNumber =  "567"),
                AltinnOrganisasjon("person ", "Person", socialSecurityNumber = "01028454321")
        )

        every {authRepoMock.hentOrgMedRettigheterForPerson(doesNotHaveAccess)} returns setOf()

        authorizer = DefaultAuthorizer(authRepoMock)
    }

    @Test
    internal fun `access Is Given To Orgnumber In the Access List`() {
        assertTrue { authorizer.hasAccess(subjectWithAccess, "123") }
    }

    @Test
    internal fun `org numbers Not In the List Is Denied`() {
        assertFalse { authorizer.hasAccess(subjectWithAccess, "666") }
    }

    @Test
    internal fun `no Access Rights Are Denied`() {
        assertFalse { authorizer.hasAccess(doesNotHaveAccess, "123") }
    }

    @Test
    internal fun `It is valid to have access to an altinn socialSecurityNumber`() {
        assertTrue { authorizer.hasAccess(subjectWithAccess, "01028454321") }
    }
}