package no.nav.helse.inntektsmeldingsvarsel

import no.altinn.schemas.services.intermediary.receipt._2009._10.ReceiptStatusEnum
import no.altinn.services.serviceengine.correspondence._2009._10.ICorrespondenceAgencyExternalBasic
import no.altinn.services.serviceengine.correspondence._2009._10.ICorrespondenceAgencyExternalBasicInsertCorrespondenceBasicV2AltinnFaultFaultFaultMessage
import org.slf4j.LoggerFactory

class AltinnVarselSender(private val altinnVarselMapper: AltinnVarselMapper, private val iCorrespondenceAgencyExternalBasic: ICorrespondenceAgencyExternalBasic,
                         private val username: String,
                         private val password: String) {

    private val log = LoggerFactory.getLogger("AltinnVarselSender")

    fun sendManglendeInnsendingAvInntektsMeldingTilArbeidsgiver(altinnVarsel: AltinnVarsel) {
        try {
            val receiptExternal = iCorrespondenceAgencyExternalBasic.insertCorrespondenceBasicV2(
                    username, password,
                    SYSTEM_USER_CODE, altinnVarsel.ressursId,
                    altinnVarselMapper.mapAltinnVarselTilInsertCorrespondence(altinnVarsel)
            )
            if (receiptExternal.receiptStatusCode != ReceiptStatusEnum.OK) {
                log.error("Fikk uventet statuskode fra Altinn {}", receiptExternal.receiptStatusCode)
                throw RuntimeException("Feil ved sending varsel om manglende innsending av sykepengesøknad til Altinn")
            }
        } catch (e: ICorrespondenceAgencyExternalBasicInsertCorrespondenceBasicV2AltinnFaultFaultFaultMessage) {
            log.error("Feil ved sending varsel om manglende innsending av sykepengesøknad til Altinn", e)
            throw RuntimeException("Feil ved sending varsel om manglende innsending av sykepengesøknad til Altinn", e)
        } catch (e: Exception) {
            log.error("Feil ved sending varsel om manglende innsending av sykepengesøknad til Altinn", e)
            throw e
        }
    }

    companion object {
        const val SYSTEM_USER_CODE = "NAV_HELSEARBEIDSGIVER"
    }

}