package no.nav.helse.spion.kafka

import com.github.javafaker.Faker
import no.nav.helse.spion.domene.Arbeidsgiver
import no.nav.helse.spion.domene.Periode
import no.nav.helse.spion.domene.Person
import no.nav.helse.spion.domene.ytelsesperiode.Ytelsesperiode
import java.time.LocalDate
import kotlin.random.Random
import kotlin.random.Random.Default.nextDouble

private class ArbeidsgiverGenerator(private val maxUniqueArbeidsgivere: Int = 1000) {
    private val faker = Faker()
    private val arbeidsgivere = mutableListOf<Arbeidsgiver>()
    fun getRandomArbeidsGiver(): Arbeidsgiver {
        return if (arbeidsgivere.size >= maxUniqueArbeidsgivere) {
            arbeidsgivere.pickRandom()
        } else {
            val arbeidsGiver = Arbeidsgiver(
                    faker.funnyName().name(),
                    Random.Default.nextLong(111111111, 999999999).toString(),
                    Random.Default.nextLong(111111111, 999999999).toString(),
                    null
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
            val person = Person(
                    faker.name().firstName(),
                    faker.name().lastName(),
                    Random.Default.nextLong(10000000000, 31120099999).toString()
            )
            persone.add(person)
            person
        }
    }
}

private val periodStateGenerator = {
    val rand = Random.Default.nextInt(0, 100)
    when {
        rand < 5 -> Ytelsesperiode.Status.AVSLÅTT
        rand < 25 -> Ytelsesperiode.Status.UNDER_BEHANDLING
        rand < 30 -> Ytelsesperiode.Status.HENLAGT
        else -> Ytelsesperiode.Status.INNVILGET
    }
}

class VedtaksmeldingGenerator(
        maxUniqueArbeidsgivere: Int,
        maxUniquePersoner: Int,
        private val initDate: LocalDate = LocalDate.of(2020, 1, 1)
) : Iterable<Vedtaksmelding>, Iterator<Vedtaksmelding> {

    private val arbeidsgiverGenerator = ArbeidsgiverGenerator(maxUniqueArbeidsgivere)
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
        val status = VedtaksmeldingsStatus.values().toList().pickRandom()

        return Vedtaksmelding(
                person.identitetsnummer,
                arbeidsgiverGenerator.getRandomArbeidsGiver().virksomhetsnummer!!,
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
