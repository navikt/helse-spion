
package no.nav.helse
import no.nav.helse.spion.web.dto.PersonOppslagDto
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class YtelsesperiodeGeneratorTest {
    @Test
    fun `Det skal komme validert testdata ut av generatoren`() {
        val generator = YtelsesperiodeGenerator(maxUniqueArbeidsgivere = 5, maxUniquePersoner = 10)

        val ypl = generator
            .take(50)
            .toList()
            .onEach { PersonOppslagDto(it.arbeidsforhold.arbeidstaker.identitetsnummer, it.arbeidsforhold.arbeidsgiver.arbeidsgiverId) }

        assertThat(ypl).hasSize(50)
    }
}
