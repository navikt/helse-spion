package no.nav.helse.spion.vedtaksmelding

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import no.nav.helse.spion.bakgrunnsjobb.BakgrunnsjobbProsesserer
import java.time.LocalDateTime

class VedtaksmeldingProcessor(
        val vedtaksmeldingService: VedtaksmeldingService,
        val om: ObjectMapper
): BakgrunnsjobbProsesserer {

companion object {
    val JOBB_TYPE = "vedtaksmelding"
}

    override fun prosesser(jobbData: String) {
        vedtaksmeldingService.processAndSaveMessage(om.readValue(jobbData, MessageWithOffset::class.java))
    }

    override fun nesteForsoek(forsoek: Int, forrigeForsoek: LocalDateTime): LocalDateTime {
        return LocalDateTime.now().plusHours((forsoek * forsoek).toLong())
    }
}
