package no.nav.helse.spion.vedtaksmelding

import com.fasterxml.jackson.databind.ObjectMapper
import no.nav.helse.spion.domene.Arbeidsgiver
import no.nav.helse.spion.domene.Periode
import no.nav.helse.spion.domene.Person
import no.nav.helse.spion.domene.ytelsesperiode.Arbeidsforhold
import no.nav.helse.spion.domene.ytelsesperiode.Ytelsesperiode
import no.nav.helse.spion.domene.ytelsesperiode.repository.YtelsesperiodeRepository
import no.nav.helse.spion.integrasjon.pdl.NameProvider
import org.slf4j.LoggerFactory
import java.time.LocalDate

class VedtaksmeldingService(
        private val ypRepo: YtelsesperiodeRepository,
        private val om: ObjectMapper,
        private val pdl: NameProvider
) {
    val log = LoggerFactory.getLogger(VedtaksmeldingService::class.java)

    fun processAndSaveMessage(melding: SpleisMelding) {
        log.info(melding.toString())
        when(melding.type) {
            SpleisMeldingstype.Behandlingstilstand.name -> processBehandlingstilstand()
            SpleisMeldingstype.Vedtak.name -> processVedtak(melding)
            else -> { /* ignorer andre meldingstyper */ }
        }
    }

    private fun processVedtak(melding: SpleisMelding) {
        val vedtak = om.readValue(melding.messageBody, SpleisVedtakDto::class.java)
        val person = pdl.fnrToName(melding.key) ?: NameProvider.Name("Ukjent",  "Ukjent")

        map(vedtak, melding.offset, melding.key, person.firstname, person.lastname)
                .forEach {
                    ypRepo.upsert(it)
                }
    }

    private fun processBehandlingstilstand() {
        // https://github.com/navikt/helse-sporbar/blob/master/src/main/kotlin/no/nav/helse/sporbar/VedtaksperiodeDto.kt
        // preliminær periode der fom tom er ukjent?
        // Bruk Sykepengesøknad-teamet sitt API for å hente ut søknaden og bruk FOM-TOM fra denne

    }

    fun map(vedtak: SpleisVedtakDto, kafkaOffset: Long, fnr: String, fornavn: String, etternavn: String): List<Ytelsesperiode> {
        return vedtak.utbetalinger
                .filter {it.fagområde == "SPREF"} // utbetalingen er en refusjon
                .flatMap {
                    it.utbetalingslinjer.map { utbetalingslinje ->
                        Ytelsesperiode(
                                Periode(utbetalingslinje.fom, utbetalingslinje.tom),
                                kafkaOffset,
                                vedtak.forbrukteSykedager,
                                vedtak.gjenståendeSykedager,
                                Arbeidsforhold("",
                                        Person(fornavn, etternavn, fnr),
                                        Arbeidsgiver(it.mottaker)),
                                (utbetalingslinje.beløp * utbetalingslinje.sykedager).toBigDecimal(),
                                Ytelsesperiode.Status.INNVILGET,
                                utbetalingslinje.grad.toBigDecimal(),
                                utbetalingslinje.beløp.toBigDecimal(),
                                Ytelsesperiode.Ytelse.SP,
                                LocalDate.now()
                        )
                    }
                }
    }
}