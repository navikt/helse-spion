package no.nav.helse.spion.vedtaksmelding

import com.fasterxml.jackson.databind.ObjectMapper
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
import java.util.*

class VedtaksmeldingProcessor(
        val kafkaVedtaksProvider: KafkaMessageProvider,
        val ypDao: YtelsesperiodeRepository,
        val failedVedtaksmeldingRepository: FailedVedtaksmeldingRepository,
        val om: ObjectMapper,
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

    fun processOneMessage(melding: String) {
        try {
            val deserializedKafkaMessage = om.readValue(melding, Vedtaksmelding::class.java)
            val mapped = mapVedtaksMeldingTilYtelsesPeriode(deserializedKafkaMessage)
            ypDao.save(mapped)
        } catch (t: Throwable) {
            val errorId = UUID.randomUUID()
            logger.error("Feilet vedtaksmelding, ID: $errorId", t)
            failedVedtaksmeldingRepository.save(FailedVedtaksmelding(
                    melding, t.message, errorId
            ))
        }
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