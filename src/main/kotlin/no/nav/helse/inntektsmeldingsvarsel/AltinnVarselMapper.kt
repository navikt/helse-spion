package no.nav.helse.inntektsmeldingsvarsel

import no.altinn.schemas.services.serviceengine.correspondence._2010._10.ExternalContentV2
import no.altinn.schemas.services.serviceengine.correspondence._2010._10.InsertCorrespondenceV2
import no.altinn.schemas.services.serviceengine.notification._2009._10.NotificationBEList
import no.nav.helse.inntektsmeldingsvarsel.NotificationAltinnGenerator.lenkeAltinnPortal
import no.nav.helse.inntektsmeldingsvarsel.NotificationAltinnGenerator.opprettEpostNotification
import no.nav.helse.inntektsmeldingsvarsel.NotificationAltinnGenerator.opprettSMSNotification
import no.nav.helse.inntektsmeldingsvarsel.NotificationAltinnGenerator.smsLenkeAltinnPortal
import java.time.format.DateTimeFormatter
import javax.xml.bind.JAXBElement
import javax.xml.namespace.QName

class AltinnVarselMapper {
    fun mapAltinnVarselTilInsertCorrespondence(altinnVarsel: AltinnVarsel): InsertCorrespondenceV2 {
        val formatter = DateTimeFormatter.ofPattern("dd.MM.yyyy")
        val namespace = "http://schemas.altinn.no/services/ServiceEngine/Correspondence/2010/10"
        val tittel = "Manglende inntektsmelding i forbindelse med sykepenger - " + altinnVarsel.navnSykmeldt + " (" + altinnVarsel.fnrSykmeldt + ")"
        val innhold = "" +
                "<html>\n" +
                "   <head>\n" +
                "       <meta charset=\"UTF-8\">\n" +
                "   </head>\n" +
                "   <body>\n" +
                "       <div class=\"melding\">\n" +
                "           <h2>DETTE ER EN TEST</h2>\n" +
                "           <p>" + altinnVarsel.navnSykmeldt + " (" + altinnVarsel.fnrSykmeldt + ") har fått en søknad om sykepenger til utfylling, men har foreløpig ikke sendt den inn.</p>\n" +
                "           <p>Perioden søknaden gjelder for er " + formatter.format(altinnVarsel.soknadFom) + "-" + formatter.format(altinnVarsel.soknadTom) + "</p>\n" +
                "           <h4>Om denne meldingen:</h4>\n" +
                "           <p>Denne meldingen er automatisk generert og skal hjelpe arbeidsgivere med å få oversikt over sykepengesøknader som mangler. NAV påtar seg ikke ansvar for eventuell manglende påminnelse. Vi garanterer heller ikke for at foreldelsesfristen ikke er passert, eller om det er andre grunner til at retten til sykepenger ikke er oppfylt. Dersom arbeidstakeren har åpnet en søknad og avbrutt den, vil det ikke bli sendt melding til dere.</p>\n" +
                "       </div>\n" +
                "   </body>\n" +
                "</html>"
        val meldingsInnhold = ExternalContentV2()
                .withLanguageCode("1044")
                .withMessageTitle(tittel)
                .withMessageBody(innhold)
        return InsertCorrespondenceV2()
                .withAllowForwarding(false)
                .withReportee(altinnVarsel.orgnummer)
                .withMessageSender("NAV (Arbeids- og velferdsetaten)")
                .withServiceCode(MANGLER_INNTEKTSMELDING_TJENESTEKODE)
                .withServiceEdition(SYKEPENGESOEKNAD_TJENESTEVERSJON) //.withNotifications(opprettManglendeInnsendingNotifications(namespace))
                .withContent(meldingsInnhold)
    }

    private fun opprettManglendeInnsendingNotifications(namespace: String): JAXBElement<NotificationBEList> {
        val epost = opprettEpostNotification("Sykepengesøknad som ikke er sendt inn",
                "<p>En ansatt i \$reporteeName$ (\$reporteeNumber$) har fått en søknad om sykepenger til utfylling, men har foreløpig ikke sendt den inn.</p>" +
                        "<p>Logg inn på <a href=\"" + lenkeAltinnPortal() + "\">Altinn</a> for å se hvem det gjelder og hvilken periode søknaden gjelder for.</p>" +
                        "<p>Mer informasjon om digital sykmelding og sykepengesøknad finner du på www.nav.no/digitalsykmelding.</p>" +
                        "<p>Vennlig hilsen NAV</p>")
        val sms = opprettSMSNotification(
                "En ansatt i \$reporteeName$ (\$reporteeNumber$) har fått en søknad om sykepenger til utfylling, men har foreløpig ikke sendt den inn.",
                "Gå til meldingsboksen i " + smsLenkeAltinnPortal() + " for å se hvem det gjelder og hvilken periode søknaden gjelder for. \n\nVennlig hilsen NAV"
        )
        return JAXBElement(QName(namespace, "Notifications"), NotificationBEList::class.java, NotificationBEList()
                .withNotification(epost, sms))
    }

    companion object {
        private const val MANGLER_INNTEKTSMELDING_TJENESTEKODE = "5534" // OBS! VIKTIG! Denne må ikke endres, da kan feil personer få tilgang i Altinn!
        private const val SYKEPENGESOEKNAD_TJENESTEVERSJON = "1"
    }
}