package no.nav.helse.spion.kafka

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import org.apache.kafka.common.serialization.Deserializer
import org.apache.kafka.common.serialization.Serializer
import java.time.LocalDate

enum class SaksYtelse { SP, FP, SVP, PP, OP, OM }
enum class SaksStatus { BEHANDLES, INNVILGET, AVLÅTT, HENLAGT }

data class VedtaksMelding(
        val identitetsNummer: String,
        val virksomhetsnummer: String,
        val status: SaksStatus,
        val fom: LocalDate,
        val tom: LocalDate,
        val ytelse: SaksYtelse = SaksYtelse.SP,

        val fornavn: String,
        val etternavn: String,

        val sykemeldingsgrad: Int?,
        val refusjonsbeløp: Double
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
internal class VedtaksMeldingSerDes(om: ObjectMapper) : SerDes<VedtaksMelding>(om) {
    override fun deserialize(topic: String?, data: ByteArray?): VedtaksMelding? {
        return data?.let {
            om.readValue<VedtaksMelding>(it)
        }
    }
}