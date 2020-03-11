package no.nav.helse.spion.vedtaksmelding

import com.github.javafaker.Faker
import no.nav.helse.spion.domene.Arbeidsgiver
import no.nav.helse.spion.domene.Periode
import no.nav.helse.spion.domene.Person
import no.nav.helse.spion.web.dto.validation.FoedselsNrValidator
import no.nav.helse.spion.web.dto.validation.FoedselsNrValidator.Companion.tabeller.kontrollsiffer1
import no.nav.helse.spion.web.dto.validation.FoedselsNrValidator.Companion.tabeller.kontrollsiffer2
import no.nav.helse.spion.web.dto.validation.OrganisasjonsnummerValidator
import no.nav.helse.spion.web.dto.validation.OrganisasjonsnummerValidator.Companion.tabeller.weights
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import kotlin.random.Random
import kotlin.random.Random.Default.nextDouble
import kotlin.random.Random.Default.nextInt
import kotlin.random.Random.Default.nextLong

private class ArbeidsgiverGenerator(fixedList: MutableList<Arbeidsgiver>? = null, private var maxUniqueArbeidsgivere: Int = 1000) {
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
            orgNr += OrganisasjonsnummerValidator.checksum(weights, orgNr)
            var virkNr = Random.Default.nextLong(11111111, 99999999).toString()
            virkNr += OrganisasjonsnummerValidator.checksum(weights, orgNr)

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

private class PersonGenerator(private val maxUniquePersons: Int = 1000) {
    private val faker = Faker()
    private val persone = mutableListOf<Person>()
    fun getRandomPerson(): Person {
        return if (persone.size >= maxUniquePersons) {
            persone.pickRandom()
        } else {
            val tenYearsBeforeAfterEpoch = 3600 * 24 * 365 * 10L
            val birthDay = LocalDate.ofInstant(Instant.ofEpochSecond(nextLong(-tenYearsBeforeAfterEpoch, tenYearsBeforeAfterEpoch)), ZoneId.systemDefault())
            var fnr = birthDay.format(DateTimeFormatter.ofPattern("ddMMyy")) + nextInt(100, 999)
            fnr += FoedselsNrValidator.checksum(kontrollsiffer1, fnr)
            fnr += FoedselsNrValidator.checksum(kontrollsiffer2, fnr)

            val person = Person(
                    faker.name().firstName(),
                    faker.name().lastName(),
                    fnr
            )
            persone.add(person)
            person
        }
    }
}

private val periodStateGenerator = {
    val rand = Random.Default.nextInt(0, 100)
    when {
        rand < 5 -> VedtaksmeldingsStatus.AVSLÅTT
        rand < 25 -> VedtaksmeldingsStatus.BEHANDLES
        rand < 30 -> VedtaksmeldingsStatus.HENLAGT
        else -> VedtaksmeldingsStatus.INNVILGET
    }
}

class VedtaksmeldingGenerator(
        fixedListArbeidsgivere: MutableList<Arbeidsgiver>? = null,
        maxUniqueArbeidsgivere: Int = 10,
        maxUniquePersoner: Int = 100,
        private val initDate: LocalDate = LocalDate.of(2020, 1, 1)
) : Iterable<Vedtaksmelding>, Iterator<Vedtaksmelding> {

    private val arbeidsgiverGenerator = ArbeidsgiverGenerator(fixedListArbeidsgivere, maxUniqueArbeidsgivere)
    private val personGenerator = PersonGenerator(maxUniquePersoner)
    private var numGeneratedVedtak = 0
    private val maxPeriodeLength = 31L;

    private fun randomPeriode(): Periode {
        val fom = initDate.plusDays(Random.Default.nextLong(0, (numGeneratedVedtak % 2000 + 1).toLong()))
        val tom = fom.plusDays(Random.Default.nextLong(1, maxPeriodeLength))
        return Periode(fom, tom)
    }

    private val sykemeldingsGrader = listOf(20, 30, 50, 80, 100)

    private fun generate(): Vedtaksmelding {
        val periode = randomPeriode()
        val person = personGenerator.getRandomPerson()
        numGeneratedVedtak++
        val status = periodStateGenerator()

        return Vedtaksmelding(
                person.identitetsnummer,
                arbeidsgiverGenerator.getRandomArbeidsGiver().arbeidsgiverId!!,
                status,
                periode.fom,
                periode.tom,
                VedtaksmeldingsYtelse.SP,
                person.fornavn,
                person.etternavn,
                if (status == VedtaksmeldingsStatus.INNVILGET) sykemeldingsGrader.pickRandom() else null,
                if (status == VedtaksmeldingsStatus.INNVILGET) nextDouble(10.0, 10000.0) else null,
                if (status == VedtaksmeldingsStatus.INNVILGET) nextDouble(10.0, 1000.0) else null,
                periode.tom
        )
    }

    override fun iterator(): Iterator<Vedtaksmelding> {
        return this
    }

    override fun hasNext(): Boolean {
        return true
    }

    override fun next(): Vedtaksmelding {
        return generate()
    }
}


private fun <E> List<E>.pickRandom(): E {
    return this[Random.Default.nextInt(0, this.size)]!!
}
