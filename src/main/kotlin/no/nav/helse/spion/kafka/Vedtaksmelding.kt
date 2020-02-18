package no.nav.helse.spion.kafka

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import no.nav.helse.spion.domene.ytelsesperiode.Ytelsesperiode
import org.apache.kafka.common.serialization.Deserializer
import org.apache.kafka.common.serialization.Serializer
import java.time.LocalDate

enum class VedtaksmeldingsYtelse { SP, FP, SVP, PP, OP, OM }
enum class VedtaksmeldingsStatus(val correspondingDomainStatus: Ytelsesperiode.Status) {
    BEHANDLES(Ytelsesperiode.Status.UNDER_BEHANDLING),
    INNVILGET(Ytelsesperiode.Status.INNVILGET),
    AVSLÅTT(Ytelsesperiode.Status.AVSLÅTT),
    HENLAGT(Ytelsesperiode.Status.HENLAGT)
}

data class Vedtaksmelding(
        val identitetsNummer: String,
        val virksomhetsnummer: String,
        val status: VedtaksmeldingsStatus,
        val fom: LocalDate,
        val tom: LocalDate,
        val ytelse: VedtaksmeldingsYtelse = VedtaksmeldingsYtelse.SP,

        val fornavn: String,
        val etternavn: String,

        val sykemeldingsgrad: Int?,
        val refusjonsbeløp: Double?,
        val dagsats: Double?,
        val maksDato: LocalDate?

)

internal abstract class SerDes<V>(protected val om: ObjectMapper) : Serializer<V>, Deserializer<V> {
    override fun serialize(topic: String?, data: V): ByteArray? {
        return data?.let {
            om.writeValueAsBytes(it)
        }
    }

    override fun configure(configs: MutableMap<String, *>?, isKey: Boolean) {}
    override fun close() {}
}

internal class VedtaksMeldingSerDes(om: ObjectMapper) : SerDes<Vedtaksmelding>(om) {
    override fun deserialize(topic: String?, data: ByteArray?): Vedtaksmelding? {
        return data?.let {
            om.readValue<Vedtaksmelding>(it)
        }
    }
}
