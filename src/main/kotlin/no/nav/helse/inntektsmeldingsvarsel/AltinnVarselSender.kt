package no.nav.helse.inntektsmeldingsvarsel

import no.altinn.schemas.services.intermediary.receipt._2009._10.ReceiptStatusEnum
import no.altinn.services.serviceengine.correspondence._2009._10.ICorrespondenceAgencyExternalBasic
import no.altinn.services.serviceengine.correspondence._2009._10.ICorrespondenceAgencyExternalBasicInsertCorrespondenceBasicV2AltinnFaultFaultFaultMessage
import no.nav.helse.spion.domene.varsling.Varsling
import no.nav.helse.spion.varsling.VarslingSender
import org.slf4j.LoggerFactory

class AltinnVarselSender(private val altinnVarselMapper: AltinnVarselMapper,
                              private val iCorrespondenceAgencyExternalBasic: ICorrespondenceAgencyExternalBasic,
                              private val username: String,
                              private val password: String) : VarslingSender {

    private val log = LoggerFactory.getLogger("AltinnVarselSender")

    companion object {
        const val SYSTEM_USER_CODE = "NAV_HELSEARBEIDSGIVER"
    }

    override fun send(varsling: Varsling) {
        try {
            val receiptExternal = iCorrespondenceAgencyExternalBasic.insertCorrespondenceBasicV2(
                    username, password,
                    SYSTEM_USER_CODE, varsling.uuid,
                    altinnVarselMapper.mapVarslingTilInsertCorrespondence(varsling)
            )
            if (receiptExternal.receiptStatusCode != ReceiptStatusEnum.OK) {
                log.error("Fikk uventet statuskode fra Altinn {}", receiptExternal.receiptStatusCode)
                throw RuntimeException("Feil ved sending varsel om manglende innsending av sykepengesøknad til Altinn")
            }
        } catch (e: ICorrespondenceAgencyExternalBasicInsertCorrespondenceBasicV2AltinnFaultFaultFaultMessage) {
            log.error("Feil ved sending varsel om manglende innsending av inntektsmelding til Altinn", e)
            throw RuntimeException("Feil ved sending varsel om manglende innsending av inntektsmelding til Altinn", e)
        } catch (e: Exception) {
            log.error("Feil ved sending varsel om manglende innsending av inntektsmelding til Altinn", e)
            throw e
        }
    }
}