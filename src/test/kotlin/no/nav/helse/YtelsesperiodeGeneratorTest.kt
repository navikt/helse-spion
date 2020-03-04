
package no.nav.helse
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class YtelsesperiodeGeneratorTest {
    @Test
    fun `Det skal komme ut data av generatoren`() {
        val generator = YtelsesperiodeGenerator(2, 5, 10)

        val ypl = generator
                .take(50)
                .toList()
                .onEach { println(it) }

        assertThat(ypl).hasSize(50)
    }
}