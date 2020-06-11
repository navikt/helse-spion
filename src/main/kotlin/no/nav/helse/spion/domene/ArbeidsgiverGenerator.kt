package no.nav.helse.spion.domene

import com.github.javafaker.Faker
import no.nav.helse.spion.web.dto.validation.OrganisasjonsnummerValidator
import kotlin.random.Random

class ArbeidsgiverGenerator(
        fixedList: MutableList<Arbeidsgiver>? = null,
        private var maxUniqueArbeidsgivere: Int = 1000) {

    private val faker = Faker()
    private val arbeidsgivere = fixedList ?: mutableListOf()

    init {
        if (fixedList != null) {
            maxUniqueArbeidsgivere = fixedList.size
        }
    }

    fun getRandomArbeidsGiver(): Arbeidsgiver {
        return if (arbeidsgivere.size >= maxUniqueArbeidsgivere) {
            arbeidsgivere.pickRandom()
        } else {
            var orgNr = Random.Default.nextLong(11111111, 99999999).toString()
            orgNr += OrganisasjonsnummerValidator.checksum(OrganisasjonsnummerValidator.Companion.tabeller.weights, orgNr)
            var virkNr = Random.Default.nextLong(11111111, 99999999).toString()
            virkNr += OrganisasjonsnummerValidator.checksum(OrganisasjonsnummerValidator.Companion.tabeller.weights, virkNr)

            val arbeidsGiver = Arbeidsgiver(
                    faker.funnyName().name(),
                    orgNr,
                    virkNr
            )
            arbeidsgivere.add(arbeidsGiver)
            arbeidsGiver
        }
    }
}

private fun <E> List<E>.pickRandom(): E {
    return this[Random.Default.nextInt(0, this.size)]!!
}