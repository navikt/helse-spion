package no.nav.helse.inntektsmeldingsvarsel;

import no.altinn.schemas.serviceengine.formsengine._2009._10.TransportType;
import no.altinn.schemas.services.serviceengine.notification._2009._10.*;

import java.util.function.Function;

import static java.lang.System.getProperty;
import static java.util.Optional.ofNullable;
import static no.altinn.schemas.serviceengine.formsengine._2009._10.TransportType.EMAIL;
import static no.altinn.schemas.serviceengine.formsengine._2009._10.TransportType.SMS;

class NotificationAltinnGenerator {
    private static final String NORSK_BOKMAL = "1044";
    private static final String FRA_EPOST_ALTINN = "noreply@altinn.no";
    private static final String NOTIFICATION_NAMESPACE = "http://schemas.altinn.no/services/ServiceEngine/Notification/2009/10";

    private static String urlEncode(String lenke) {
        return lenke.replaceAll("=", "%3D");
    }

    static String smsLenkeAltinnPortal() {
        return urlEncode(lenkeAltinnPortal());
    }

    static String lenkeAltinnPortal() {
        return getProperty("altinn.portal.baseurl", "https://www.altinn.no") + "/ui/MessageBox?O=$reporteeNumber$";
    }

    static Notification opprettEpostNotification(String... text) {
        return opprettNotification(FRA_EPOST_ALTINN, EMAIL, text);
    }

    static Notification opprettSMSNotification(String... text) {
        return opprettNotification(null, SMS, text);
    }

    private static Notification opprettNotification(String fraEpost, TransportType type, String... text) {
        return opprettNotification(fraEpost, type, konverterTilTextTokens(text));
    }

    private static Notification opprettNotification(String fraEpost, TransportType type, TextToken[] textTokens) {
        if (textTokens.length != 2) {
            throw new IllegalArgumentException("Antall textTokens må være 2. Var " + textTokens.length);
        }

        return new Notification()
                .withLanguageCode(NORSK_BOKMAL)
                .withNotificationType("TokenTextOnly")
                .withFromAddress(mapNullable(fraEpost, epost -> epost))
                .withReceiverEndPoints(new ReceiverEndPointBEList()
                        .withReceiverEndPoint(new ReceiverEndPoint().withTransportType(type))
                )
                .withTextTokens(new TextTokenSubstitutionBEList().withTextToken(textTokens));
    }

    private static TextToken[] konverterTilTextTokens(String... text) {
        TextToken[] textTokens = new TextToken[text.length];
        for (int i = 0; i < text.length; i++) {
            textTokens[i] = new TextToken().withTokenNum(i).withTokenValue(text[i]);
        }
        return textTokens;
    }

    private static <T, R> R mapNullable(T fra, Function<T, R> exp) {
        return ofNullable(fra).map(exp).orElse(null);
    }
}
