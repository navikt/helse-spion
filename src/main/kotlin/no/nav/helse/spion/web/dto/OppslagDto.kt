package no.nav.helse.spion.web.dto

import no.nav.helse.spion.domene.Periode
import no.nav.helse.spion.web.dto.validation.isValidIdentitetsnummer
import no.nav.helse.spion.web.dto.validation.isValidOrganisasjonsnummer
import org.valiktor.functions.isGreaterThanOrEqualTo
import org.valiktor.functions.validate
import org.valiktor.validate


class OppslagDto(
        val identitetsnummer: String,
        val arbeidsgiverId: String,
        val periode: Periode? = null
) {
    companion object;

    init {
        validate(this) {
            validate(OppslagDto::identitetsnummer).isValidIdentitetsnummer()
            validate(OppslagDto::arbeidsgiverId).isValidOrganisasjonsnummer()

            validate(OppslagDto::periode).validate {
                validate(Periode::tom).isGreaterThanOrEqualTo(it.fom)
            }
        }
    }

}