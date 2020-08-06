package no.nav.helse.spion.vedtaksmelding

import com.fasterxml.jackson.databind.ObjectMapper
import no.nav.helse.spion.domene.Arbeidsgiver
import no.nav.helse.spion.domene.Periode
import no.nav.helse.spion.domene.Person
import no.nav.helse.spion.domene.ytelsesperiode.Arbeidsforhold
import no.nav.helse.spion.domene.ytelsesperiode.Ytelsesperiode
import no.nav.helse.spion.domene.ytelsesperiode.repository.YtelsesperiodeRepository
import no.nav.helse.spion.integrasjon.pdl.PdlClient
import no.nav.helse.spion.integrasjon.pdl.PdlPersonNavn
import java.time.LocalDate

class VedtaksmeldingService(
        private val ypRepo: YtelsesperiodeRepository,
        private val om: ObjectMapper,
        private val pdl: PdlClient
) {
    fun processAndSaveMessage(melding: SpleisMelding) {

        when(melding.type) {
            SpleisMeldingstype.Behandlingstilstand.name -> processBehandlingstilstand(melding)
            SpleisMeldingstype.Vedtak.name -> processVedtak(melding)
            else -> { /* ignorer andre meldingstyper */ }
        }
    }

    private fun processVedtak(melding: SpleisMelding) {
        val vedtak = om.readValue(melding.messageBody, SpleisVedtakDto::class.java)
        val person = pdl.person(melding.key)?.hentPerson?.navn?.firstOrNull() ?: PdlPersonNavn("Ukjent",  null, "Ukjent")
        val virksomhet = vedtak.utbetalinger.map { it.mottaker }.firstOrNull() ?: throw IllegalStateException("Vedtaket har ingen utbetalinger, kan ikke knyttes til virksomhet")

        if (virksomhet.length != 9) return // vedtaket har utbetaling til noe annet enn et organisasjonsnummer

        val mapped = map(vedtak, melding.offset, melding.key, person.fornavn, person.etternavn, virksomhet)

        ypRepo.upsert(mapped)
    }

    private fun processBehandlingstilstand(melding: SpleisMelding) {
        // https://github.com/navikt/helse-sporbar/blob/master/src/main/kotlin/no/nav/helse/sporbar/VedtaksperiodeDto.kt
        // preliminær periode der fom tom er ukjent?
        // Hvordan mappe til domene uten fom tom?
    }

    fun map(vm: SpleisVedtakDto, kafkaOffset: Long, fnr: String, fornavn: String, etternavn: String, virksomhet: String): Ytelsesperiode {
        val beloep = vm.utbetalinger.sumBy { it.totalbeløp }.toBigDecimal()

        return Ytelsesperiode(
                Periode(vm.fom, vm.tom),
                kafkaOffset,
                Arbeidsforhold("",
                        Person(fornavn, etternavn, fnr),
                        Arbeidsgiver("TODO?", virksomhet)),
                "UKJENT",
                beloep,
                Ytelsesperiode.Status.INNVILGET,
                vm.snittGrad().toBigDecimal(),
                vm.snittDagsats().toBigDecimal(),
                Ytelsesperiode.Ytelse.SP,
                null,
                LocalDate.now()
        )
    }
}