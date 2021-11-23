package no.nav.helse.spion.koin

import com.fasterxml.jackson.databind.ObjectMapper
import no.nav.helse.arbeidsgiver.integrasjoner.pdl.PdlClient
import no.nav.helse.arbeidsgiver.integrasjoner.pdl.PdlHentFullPerson
import no.nav.helse.arbeidsgiver.integrasjoner.pdl.PdlHentPersonNavn
import no.nav.helse.arbeidsgiver.integrasjoner.pdl.PdlIdent
import no.nav.helse.arbeidsgiver.integrasjoner.pdl.PdlPersonNavnMetadata
import no.nav.helse.spion.domene.Arbeidsgiver
import no.nav.helse.spion.vedtaksmelding.SpleisMelding
import no.nav.helse.spion.vedtaksmelding.SpleisVedtaksmeldingGenerator
import no.nav.helse.spion.vedtaksmelding.VedtaksmeldingProvider
import kotlin.random.Random

val createStaticPdlMock = fun(): PdlClient {
    return object : PdlClient {
        override fun fullPerson(ident: String) =
            PdlHentFullPerson(
                PdlHentFullPerson.PdlFullPersonliste(
                    emptyList(),
                    emptyList(),
                    emptyList(),
                    emptyList(),
                    emptyList(),
                    emptyList(),
                    emptyList()
                ),

                PdlHentFullPerson.PdlIdentResponse(listOf(PdlIdent("aktør-id", PdlIdent.PdlIdentGruppe.AKTORID))),

                PdlHentFullPerson.PdlGeografiskTilknytning(
                    PdlHentFullPerson.PdlGeografiskTilknytning.PdlGtType.UTLAND,
                    null,
                    null,
                    "SWE"
                )
            )

        override fun personNavn(ident: String) =
            PdlHentPersonNavn.PdlPersonNavneliste(
                listOf(
                    PdlHentPersonNavn.PdlPersonNavneliste.PdlPersonNavn(
                        "Ola",
                        "M",
                        "Avsender",
                        PdlPersonNavnMetadata("freg")
                    )
                )
            )
    }
}

val createVedtaksMeldingKafkaMock = fun(om: ObjectMapper): VedtaksmeldingProvider {
    return object : VedtaksmeldingProvider { // dum mock
        val arbeidsgivere = mutableListOf(
            Arbeidsgiver("917404437"),
            Arbeidsgiver("711485759")
        )

        val genrateMessages = false
        val generator = SpleisVedtaksmeldingGenerator(om, arbeidsgivere)
        override fun getMessagesToProcess(): List<SpleisMelding> {
            return if (genrateMessages && Random.Default.nextDouble() < 0.1)
                generator.take(Random.Default.nextInt(2, 50))
            else emptyList()
        }

        override fun confirmProcessingDone() {
            println("KafkaMock: Comitta til kafka")
        }
    }
}

val generateEmptyMock = fun(): VedtaksmeldingProvider {
    return object : VedtaksmeldingProvider { // dum mock
        override fun getMessagesToProcess(): List<SpleisMelding> {
            return emptyList()
        }

        override fun confirmProcessingDone() {
            // unødvendig i mock
        }
    }
}
