package no.nav.helse.spion.vedtaksmelding

import com.fasterxml.jackson.databind.ObjectMapper
import no.nav.helse.arbeidsgiver.bakgrunnsjobb.Bakgrunnsjobb
import no.nav.helse.arbeidsgiver.bakgrunnsjobb.BakgrunnsjobbProsesserer
import java.time.LocalDateTime

class VedtaksmeldingProcessor(
    val vedtaksmeldingService: VedtaksmeldingService,
    val om: ObjectMapper
) : BakgrunnsjobbProsesserer {

    override val type: String get() = JOBB_TYPE

    companion object {
        val JOBB_TYPE = "vedtaksmelding"
    }

    override fun prosesser(jobb: Bakgrunnsjobb) {
        vedtaksmeldingService.processAndSaveMessage(om.readValue(jobb.data, SpleisMelding::class.java))
    }

    override fun nesteForsoek(forsoek: Int, forrigeForsoek: LocalDateTime): LocalDateTime {
        return LocalDateTime.now().plusHours(2)
    }
}
