package no.nav.helse

import org.junit.Assert
import org.junit.jupiter.api.Test

class YtelsesperiodeGeneratorTest {
    @Test
    fun `Det skal komme ut data av generatoren`() {
        val generator = YtelsesperiodeGenerator(2, 10)

        val ypl = generator
                .take(50)
                .toList()
                .onEach { println(it) }

        Assert.assertEquals(50, ypl.size)
    }
}