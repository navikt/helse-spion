package no.nav.helse.inntektsmeldingsvarsel

import no.altinn.schemas.services.serviceengine.correspondence._2010._10.ExternalContentV2
import no.altinn.schemas.services.serviceengine.correspondence._2010._10.InsertCorrespondenceV2
import no.altinn.schemas.services.serviceengine.notification._2009._10.NotificationBEList
import no.nav.helse.inntektsmeldingsvarsel.NotificationAltinnGenerator.lenkeAltinnPortal
import no.nav.helse.inntektsmeldingsvarsel.NotificationAltinnGenerator.opprettEpostNotification
import no.nav.helse.inntektsmeldingsvarsel.NotificationAltinnGenerator.opprettSMSNotification
import no.nav.helse.inntektsmeldingsvarsel.NotificationAltinnGenerator.smsLenkeAltinnPortal
import no.nav.helse.spion.domene.varsling.Varsling
import java.time.format.DateTimeFormatter
import javax.xml.bind.JAXBElement
import javax.xml.namespace.QName

class AltinnVarselMapper {

    private fun opprettManglendeInnsendingNotifications(namespace: String): JAXBElement<NotificationBEList> {
        val epost = opprettEpostNotification("TEST EPOST",
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

    fun mapVarslingTilInsertCorrespondence(altinnVarsel: Varsling): InsertCorrespondenceV2 {
        val formatter = DateTimeFormatter.ofPattern("dd.MM.yyyy")
        val namespace = "http://schemas.altinn.no/services/ServiceEngine/Correspondence/2010/10"
        val tittel = "Varsel om manglende inntektsmelding ifm. søknad om sykepenger"
        
        val innhold = """
            <html>
               <head>
                   <meta charset="UTF-8">
               </head>
               <body>
                   <div class="melding">
                       <h2>Varsel om manglende inntektsmelding ifm. søknad om sykepenger</h2>
                       <p>
                        NAV mangler inntektsmelding for følgende ansatte ved virksomheten (${altinnVarsel.virksomhetsNr}). 
                        For at vi skal kunne utbetale sykepengene det søkes om må disse sendes oss så snart som mulig. 
                        Dersom dere har sendt inn disse i løpet av de siste 24 timene kan dere se bort fra dette varselet.
                        </p>
                        <p></p>
                        <p>
                            <a href="www.nav.no">Skjema for inntektsmelding RF-xxxxx finner du her</a><br>
                            Benytter dere eget HR-system for å sende inntektsmeldinger kan dere fortsatt benytte dette.
                        </p>
                        ${altinnVarsel.liste.map { 
                        """
                            <p>
                            <strong>${it.navn}</strong><br>
                            Personnummer: ${it.personnumer}<br>
                            Periode: ${it.periode.fom} - ${it.periode.tom}
                            </p>
                        """.trimIndent()
                        }.joinToString(separator = "\n")}
                        
                       <h4>Om denne meldingen:</h4>
                       <p>Denne meldingen er automatisk generert og skal hjelpe arbeidsgivere med å få oversikt over inntektsmeldinger som mangler. NAV påtar seg ikke ansvar for eventuell manglende påminnelse. Vi garanterer heller ikke for at foreldelsesfristen ikke er passert, eller om det er andre grunner til at retten til sykepenger ikke er oppfylt. Dersom arbeidstakeren har åpnet en søknad og avbrutt den, vil det ikke bli sendt melding til dere.</p>
                   </div>
               </body>
            </html>
        """.trimIndent()

        val meldingsInnhold = ExternalContentV2()
                .withLanguageCode("1044")
                .withMessageTitle(tittel)
                .withMessageBody(innhold)
        return InsertCorrespondenceV2()
                .withAllowForwarding(false)
                .withReportee(altinnVarsel.virksomhetsNr)
                .withMessageSender("NAV (Arbeids- og velferdsetaten)")
                .withServiceCode(MANGLER_INNTEKTSMELDING_TJENESTEKODE)
                .withServiceEdition(MANGLER_INNTEKTSMELDING_TJENESTEVERSJON) //.withNotifications(opprettManglendeInnsendingNotifications(namespace))
                .withContent(meldingsInnhold)
    }

    companion object {
        private const val MANGLER_INNTEKTSMELDING_TJENESTEKODE = "5534" // OBS! VIKTIG! Denne må ikke endres, da kan feil personer få tilgang i Altinn!
        private const val MANGLER_INNTEKTSMELDING_TJENESTEVERSJON = "1"
    }
}