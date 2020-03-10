package no.nav.helse

import no.nav.helse.spion.domene.Periode
import no.nav.helse.spion.web.dto.OppslagDto
import java.time.LocalDate

object TestData {
    val validIdentitetsnummer = "20015001543"
    val notValidIdentitetsnummer = "50012001987"
    val validOrgNr = "123456785"
    val notValidOrgNr = "123456789"
}


fun OppslagDto.Companion.validWithoutPeriode() = OppslagDto(TestData.validIdentitetsnummer, TestData.validOrgNr)
fun OppslagDto.Companion.validWithPeriode(fom: LocalDate, tom: LocalDate) = OppslagDto(TestData.validIdentitetsnummer, TestData.validOrgNr, Periode(fom, tom))
