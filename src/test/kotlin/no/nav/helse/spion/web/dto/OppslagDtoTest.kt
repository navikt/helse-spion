package no.nav.helse.spion.web.dto

import org.assertj.core.api.Assertions
import org.junit.jupiter.api.Test
import org.valiktor.ConstraintViolationException

internal class OppslagDtoTest {

    @Test
    fun validate() {
        Assertions.assertThatExceptionOfType(ConstraintViolationException::class.java).isThrownBy {
            OppslagDto("939393", "123456789").validate()
        }
    }
}