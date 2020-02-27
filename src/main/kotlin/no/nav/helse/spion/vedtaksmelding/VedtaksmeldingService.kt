package no.nav.helse.spion.vedtaksmelding

import com.fasterxml.jackson.databind.ObjectMapper
import no.nav.helse.spion.domene.Arbeidsgiver
import no.nav.helse.spion.domene.Periode
import no.nav.helse.spion.domene.Person
import no.nav.helse.spion.domene.ytelsesperiode.Arbeidsforhold
import no.nav.helse.spion.domene.ytelsesperiode.Ytelsesperiode
import no.nav.helse.spion.domene.ytelsesperiode.repository.YtelsesperiodeRepository
import java.time.LocalDate

class VedtaksmeldingService(
        private val ypRepo: YtelsesperiodeRepository,
        private val om: ObjectMapper
) {

    fun processAndSaveMessage(melding: String) {
        val deserializedKafkaMessage = om.readValue(melding, Vedtaksmelding::class.java)
        val mapped = mapVedtaksMeldingTilYtelsesPeriode(deserializedKafkaMessage)
        ypRepo.save(mapped)
    }

    companion object Mapper {
        fun mapVedtaksMeldingTilYtelsesPeriode(vm: Vedtaksmelding): Ytelsesperiode {
            return Ytelsesperiode(
                    Periode(vm.fom, vm.tom),
                    Arbeidsforhold("",
                            Person(vm.fornavn, vm.etternavn, vm.identitetsNummer),
                            Arbeidsgiver("TODO?", "TODO?", vm.virksomhetsnummer)),
                    "UKJENT",
                    vm.refusjonsbeløp?.toBigDecimal(),
                    vm.status.correspondingDomainStatus,
                    vm.sykemeldingsgrad?.toBigDecimal(),
                    vm.dagsats?.toBigDecimal(),
                    vm.maksDato,
                    listOf(),
                    Ytelsesperiode.Ytelse.SP,
                    "INGEN MERKNAD",
                    LocalDate.now()
            )
        }
    }
}