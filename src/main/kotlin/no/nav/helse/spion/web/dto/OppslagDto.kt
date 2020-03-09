package no.nav.helse.spion.web.dto

import no.nav.helse.spion.domene.Periode
import no.nav.helse.spion.web.dto.validation.Validable
import no.nav.helse.spion.web.dto.validation.isValidIdentitetsnummer
import org.valiktor.functions.hasSize
import org.valiktor.functions.isGreaterThanOrEqualTo
import org.valiktor.functions.matches
import org.valiktor.functions.validate
import org.valiktor.validate


data class OppslagDto(
        val identitetsnummer: String,
        val arbeidsgiverId: String,
        val periode: Periode? = null
) : Validable {
    override fun validate() {
        validate(this) {
            validate(OppslagDto::identitetsnummer).isValidIdentitetsnummer()
            validate(OppslagDto::arbeidsgiverId).hasSize(min = 9, max = 9).matches(Regex("\\d+"))

            validate(OppslagDto::periode).validate {
                validate(Periode::tom).isGreaterThanOrEqualTo(it.fom)
            }
        }
    }
}