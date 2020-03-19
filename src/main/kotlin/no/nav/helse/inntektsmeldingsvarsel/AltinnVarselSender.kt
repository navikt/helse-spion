package no.nav.helse.inntektsmeldingsvarsel;

import no.altinn.schemas.services.intermediary.receipt._2009._10.ReceiptExternal;
import no.altinn.schemas.services.intermediary.receipt._2009._10.ReceiptStatusEnum;
import no.altinn.services.serviceengine.correspondence._2009._10.ICorrespondenceAgencyExternalBasic;
import no.altinn.services.serviceengine.correspondence._2009._10.ICorrespondenceAgencyExternalBasicInsertCorrespondenceBasicV2AltinnFaultFaultFaultMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AltinnVarselSender {
    static final String SYSTEM_USER_CODE = "NAV_HELSEARBEIDSGIVER";
    private final AltinnVarselMapper altinnVarselMapper;
    private final ICorrespondenceAgencyExternalBasic iCorrespondenceAgencyExternalBasic;
    private final String username;
    private final String password;

    private final Logger log = LoggerFactory.getLogger("Altinvarsler");

    public AltinnVarselSender(AltinnVarselMapper altinnVarselMapper, ICorrespondenceAgencyExternalBasic iCorrespondenceAgencyExternalBasic,
                              String username,
                              String password) {
        this.altinnVarselMapper = altinnVarselMapper;
        this.iCorrespondenceAgencyExternalBasic = iCorrespondenceAgencyExternalBasic;
        this.username = username;
        this.password = password;
    }

    public void sendManglendeInnsendingAvInntektsMeldingTilArbeidsgiver(AltinnVarsel altinnVarsel) {
        try {
            ReceiptExternal receiptExternal = iCorrespondenceAgencyExternalBasic.insertCorrespondenceBasicV2(
                    username, password,
                    SYSTEM_USER_CODE, altinnVarsel.getRessursId(),
                    altinnVarselMapper.mapAltinnVarselTilInsertCorrespondence(altinnVarsel)
            );
            if (receiptExternal.getReceiptStatusCode() != ReceiptStatusEnum.OK) {
                log.error("Fikk uventet statuskode fra Altinn {}", receiptExternal.getReceiptStatusCode());
                throw new RuntimeException("Feil ved sending varsel om manglende innsending av sykepengesøknad til Altinn");
            }
        } catch (ICorrespondenceAgencyExternalBasicInsertCorrespondenceBasicV2AltinnFaultFaultFaultMessage e) {
            log.error("Feil ved sending varsel om manglende innsending av sykepengesøknad til Altinn", e);
            throw new RuntimeException("Feil ved sending varsel om manglende innsending av sykepengesøknad til Altinn", e);
        } catch (Exception e) {
            log.error("Feil ved sending varsel om manglende innsending av sykepengesøknad til Altinn", e);
            throw e;
        }
    }
}
