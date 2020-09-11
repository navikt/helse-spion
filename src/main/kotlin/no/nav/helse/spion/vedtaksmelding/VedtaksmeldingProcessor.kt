package no.nav.helse.spion.vedtaksmelding

import com.fasterxml.jackson.databind.ObjectMapper
import no.nav.helse.arbeidsgiver.bakgrunnsjobb.BakgrunnsjobbProsesserer
import java.time.LocalDateTime

class VedtaksmeldingProcessor(
        val vedtaksmeldingService: VedtaksmeldingService,
        val om: ObjectMapper
) : BakgrunnsjobbProsesserer {

    companion object {
        val JOBB_TYPE = "vedtaksmelding"
    }

    override fun prosesser(jobbData: String) {
        vedtaksmeldingService.processAndSaveMessage(om.readValue(jobbData, SpleisMelding::class.java))
    }

    override fun nesteForsoek(forsoek: Int, forrigeForsoek: LocalDateTime): LocalDateTime {
        return LocalDateTime.now().plusHours(2)
    }
}
