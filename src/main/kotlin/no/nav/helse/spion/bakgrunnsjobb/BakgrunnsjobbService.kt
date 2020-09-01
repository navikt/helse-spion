package no.nav.helse.spion.bakgrunnsjobb

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import no.nav.helse.spion.vedtaksmelding.VedtaksmeldingProcessor
import org.slf4j.LoggerFactory
import java.time.LocalDateTime

class BakgrunnsjobbService(
        val bakgrunnsjobbRepository: BakgrunnsjobbRepository,
        val delayMillis: Long,
        vedtaksmeldingProcessor: VedtaksmeldingProcessor
) {

    private val prossesserere = HashMap<String, BakgrunnsjobbProsesserer>()
    private val logger: org.slf4j.Logger = LoggerFactory.getLogger("BakgrunnsjobbService")
    private val coroutineScope = CoroutineScope(Dispatchers.IO)

    init {
        prossesserere[VedtaksmeldingProcessor.JOBB_TYPE] = vedtaksmeldingProcessor
        sjekkOgProsseserVentendeBakgrunnsjobber()
    }


    fun sjekkOgProsseserVentendeBakgrunnsjobber() {
        coroutineScope.launch {
            finnVentende()
                    .also { logger.info("Fant ${it.size} bakgrunnsjobber å kjøre") }
                    .forEach{prosesser(it)}
            delay(delayMillis)
            sjekkOgProsseserVentendeBakgrunnsjobber()
        }
    }

    fun prosesser(jobb: Bakgrunnsjobb) {
        jobb.behandlet = LocalDateTime.now()
        jobb.forsoek++

        try {
            val prossessorForType = prossesserere[jobb.type]
                    ?: throw IllegalArgumentException("Det finnes ingen prossessor for typen '${jobb.type}'. Dette må konfigureres.")

            jobb.kjoeretid = prossessorForType.nesteForsoek(jobb.forsoek, LocalDateTime.now())
            prossessorForType.prosesser(jobb.data)
            jobb.status = BakgrunnsjobbStatus.OK
        } catch (ex: Exception) {
            jobb.status = if (jobb.forsoek >= jobb.maksAntallForsoek) BakgrunnsjobbStatus.STOPPET else BakgrunnsjobbStatus.FEILET
            if (jobb.status == BakgrunnsjobbStatus.STOPPET) {
                logger.error("Jobb ${jobb.uuid} feilet permanent", ex)
            } else {
                logger.error("Jobb ${jobb.uuid} feilet, forsøker igjen ${jobb.kjoeretid}", ex)
            }
        } finally {
            bakgrunnsjobbRepository.update(jobb)
        }
    }


    fun finnVentende(): List<Bakgrunnsjobb> =
            bakgrunnsjobbRepository.findByKjoeretidBeforeAndStatusIn(LocalDateTime.now(), setOf(BakgrunnsjobbStatus.OPPRETTET, BakgrunnsjobbStatus.FEILET))
}


/**
 * Interface for en klasse som kan prosessere en bakgrunnsjobbstype
 */
interface BakgrunnsjobbProsesserer {
    fun prosesser(jobbData: String)
    fun nesteForsoek(forsoek: Int, forrigeForsoek: LocalDateTime): LocalDateTime
}
