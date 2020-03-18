package no.nav.helse.inntektsmeldingsvarsel;

import no.altinn.schemas.services.serviceengine.correspondence._2010._10.ExternalContentV2;
import no.altinn.schemas.services.serviceengine.correspondence._2010._10.InsertCorrespondenceV2;
import no.altinn.schemas.services.serviceengine.notification._2009._10.Notification;
import no.altinn.schemas.services.serviceengine.notification._2009._10.NotificationBEList;

import javax.xml.bind.JAXBElement;
import javax.xml.namespace.QName;
import java.time.format.DateTimeFormatter;

import static no.nav.helse.inntektsmeldingsvarsel.NotificationAltinnGenerator.*;

class AltinnVarselMapper {
    private static final String MANGLER_INNTEKTSMELDING_TJENESTEKODE = "5534"; // OBS! VIKTIG! Denne må ikke endres, da kan feil personer få tilgang i Altinn!
    private static final String SYKEPENGESOEKNAD_TJENESTEVERSJON = "1";

    InsertCorrespondenceV2 mapAltinnVarselTilInsertCorrespondence(AltinnVarsel altinnVarsel) {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd.MM.yyyy");

        String namespace = "http://schemas.altinn.no/services/ServiceEngine/Correspondence/2010/10";

        String tittel = "Manglende inntektsmelding i forbindelse med sykepenger - " + altinnVarsel.getNavnSykmeldt() + " (" + altinnVarsel.getFnrSykmeldt() + ")";
        String innhold = "" +
                "<html>\n" +
                "   <head>\n" +
                "       <meta charset=\"UTF-8\">\n" +
                "   </head>\n" +
                "   <body>\n" +
                "       <div class=\"melding\">\n" +
                "           <h2>Søknad om sykepenger</h2>\n" +
                "           <p>" + altinnVarsel.getNavnSykmeldt() + " (" + altinnVarsel.getFnrSykmeldt() + ") har fått en søknad om sykepenger til utfylling, men har foreløpig ikke sendt den inn.</p>\n" +
                "           <p>Perioden søknaden gjelder for er " + formatter.format(altinnVarsel.getSoknadFom()) + "-" + formatter.format(altinnVarsel.getSoknadTom()) + "</p>\n" +
                "           <h4>Om denne meldingen:</h4>\n" +
                "           <p>Denne meldingen er automatisk generert og skal hjelpe arbeidsgivere med å få oversikt over sykepengesøknader som mangler. NAV påtar seg ikke ansvar for eventuell manglende påminnelse. Vi garanterer heller ikke for at foreldelsesfristen ikke er passert, eller om det er andre grunner til at retten til sykepenger ikke er oppfylt. Dersom arbeidstakeren har åpnet en søknad og avbrutt den, vil det ikke bli sendt melding til dere.</p>\n" +
                "       </div>\n" +
                "   </body>\n" +
                "</html>";

        ExternalContentV2 meldingsInnhold = new ExternalContentV2()
                .withLanguageCode("1044")
                .withMessageTitle(tittel)
                .withMessageBody(innhold);

        return new InsertCorrespondenceV2()
                .withAllowForwarding(false)
                .withReportee(altinnVarsel.getOrgnummer())
                .withMessageSender("NAV (Arbeids- og velferdsetaten)")
                .withServiceCode(MANGLER_INNTEKTSMELDING_TJENESTEKODE)
                .withServiceEdition(SYKEPENGESOEKNAD_TJENESTEVERSJON)
                //.withNotifications(opprettManglendeInnsendingNotifications(namespace))
                .withContent(meldingsInnhold);
    }

    private JAXBElement<NotificationBEList> opprettManglendeInnsendingNotifications(String namespace) {
        Notification epost = opprettEpostNotification("Sykepengesøknad som ikke er sendt inn",
                "<p>En ansatt i $reporteeName$ ($reporteeNumber$) har fått en søknad om sykepenger til utfylling, men har foreløpig ikke sendt den inn.</p>" +
                        "<p>Logg inn på <a href=\"" + lenkeAltinnPortal() + "\">Altinn</a> for å se hvem det gjelder og hvilken periode søknaden gjelder for.</p>" +
                        "<p>Mer informasjon om digital sykmelding og sykepengesøknad finner du på www.nav.no/digitalsykmelding.</p>" +
                        "<p>Vennlig hilsen NAV</p>");

        Notification sms = opprettSMSNotification(
                "En ansatt i $reporteeName$ ($reporteeNumber$) har fått en søknad om sykepenger til utfylling, men har foreløpig ikke sendt den inn.",
                "Gå til meldingsboksen i " + smsLenkeAltinnPortal() + " for å se hvem det gjelder og hvilken periode søknaden gjelder for. \n\nVennlig hilsen NAV"
        );

        return new JAXBElement<>(new QName(namespace, "Notifications"), NotificationBEList.class, new NotificationBEList()
                .withNotification(epost, sms));
    }
}
