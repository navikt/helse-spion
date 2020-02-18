package no.nav.helse.spion.kafka

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import no.nav.helse.spion.domene.Arbeidsgiver
import no.nav.helse.spion.domene.Periode
import no.nav.helse.spion.domene.Person
import no.nav.helse.spion.domene.ytelsesperiode.Arbeidsforhold
import no.nav.helse.spion.domene.ytelsesperiode.Ytelsesperiode
import no.nav.helse.spion.domene.ytelsesperiode.repository.YtelsesperiodeRepository
import org.slf4j.LoggerFactory
import java.time.LocalDate

class VedtaksmeldingProcessor(
        val kafkaVedtaksProvider: KafkaMessageProvider<Vedtaksmelding>,
        val ypDao: YtelsesperiodeRepository,
        val coroutineScope: CoroutineScope,
        val waitTimeWhenEmptyQueue: Long = 30000
) {
    private val logger = LoggerFactory.getLogger(VedtaksmeldingProcessor::class.java)

    private var isRunning = false

    fun startAsync() {
        logger.debug("Starter opp")
        isRunning = true
        doPoll()
    }

    fun stop() {
        logger.debug("Stopper pollingjobben...")
        isRunning = false
    }

    private fun doPoll() {
        coroutineScope.launch {
            val queueWasEmpty = processOneBatch()

            if (queueWasEmpty) {
                delay(waitTimeWhenEmptyQueue)
            }

            if (isRunning) {
                doPoll()
            } else {
                logger.debug("Stoppet polling")
            }
        }
    }

    fun processOneBatch(): Boolean {
        val wasEmpty = kafkaVedtaksProvider.getMessagesToProcess()
                .onEach { processOneMessage(it) }
                .isEmpty()

        if (!wasEmpty) {
            kafkaVedtaksProvider.confirmProcessingDone()
        }

        return wasEmpty
    }

    fun processOneMessage(melding: Vedtaksmelding) {
        val mapped = mapVedtaksMeldingTilYtelsesPeriode(melding)
        ypDao.save(mapped)
    }
}

fun mapVedtaksMeldingTilYtelsesPeriode(vm: Vedtaksmelding): Ytelsesperiode {
    return Ytelsesperiode(
            Periode(vm.fom, vm.tom),
            Arbeidsforhold("",
                    Person(vm.fornavn, vm.etternavn, vm.identitetsNummer),
                    Arbeidsgiver("TODO?", "TODO?", vm.virksomhetsnummer, null)),
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