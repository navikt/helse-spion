package no.nav.helse.inntektsmeldingsvarsel

import no.altinn.schemas.serviceengine.formsengine._2009._10.TransportType
import no.altinn.schemas.services.serviceengine.notification._2009._10.*
import java.util.*
import java.util.function.Function

internal object NotificationAltinnGenerator {
    private const val NORSK_BOKMAL = "1044"
    private const val FRA_EPOST_ALTINN = "noreply@altinn.no"

    private fun urlEncode(lenke: String): String {
        return lenke.replace("=".toRegex(), "%3D")
    }

    @JvmStatic
    fun smsLenkeAltinnPortal(): String {
        return urlEncode(lenkeAltinnPortal())
    }

    @JvmStatic
    fun lenkeAltinnPortal(): String {
        return "https://www.altinn.no/ui/MessageBox?O=\$reporteeNumber$"
    }

    @JvmStatic
    fun opprettEpostNotification(vararg text: String): Notification {
        return opprettNotification(FRA_EPOST_ALTINN, TransportType.EMAIL, *text)
    }

    @JvmStatic
    fun opprettSMSNotification(vararg text: String): Notification {
        return opprettNotification(null, TransportType.SMS, *text)
    }

    private fun opprettNotification(fraEpost: String?, type: TransportType, vararg text: String): Notification {
        return opprettNotification(fraEpost, type, konverterTilTextTokens(*text))
    }

    private fun opprettNotification(fraEpost: String?, type: TransportType, textTokens: Array<TextToken?>): Notification {
        require(textTokens.size == 2) { "Antall textTokens må være 2. Var " + textTokens.size }
        return Notification()
                .withLanguageCode(NORSK_BOKMAL)
                .withNotificationType("TokenTextOnly")
                .withFromAddress(mapNullable(fraEpost, Function { epost: String? -> epost }))
                .withReceiverEndPoints(ReceiverEndPointBEList()
                        .withReceiverEndPoint(ReceiverEndPoint().withTransportType(type))
                )
                .withTextTokens(TextTokenSubstitutionBEList().withTextToken(*textTokens))
    }

    private fun konverterTilTextTokens(vararg text: String): Array<TextToken?> {
        val textTokens = arrayOfNulls<TextToken>(text.size)
        for (i in 0 until text.size) {
            textTokens[i] = TextToken().withTokenNum(i).withTokenValue(text[i])
        }
        return textTokens
    }

    private fun <T, R> mapNullable(fra: T, exp: Function<T, R>): R {
        return Optional.ofNullable(fra).map(exp).orElse(null)
    }
}