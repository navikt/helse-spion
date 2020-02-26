package no.nav.helse

import com.github.javafaker.Faker
import no.nav.helse.spion.domene.Arbeidsgiver
import no.nav.helse.spion.domene.Periode
import no.nav.helse.spion.domene.Person
import no.nav.helse.spion.domene.ytelsesperiode.Arbeidsforhold
import no.nav.helse.spion.domene.ytelsesperiode.Ytelsesperiode
import java.math.BigDecimal
import java.time.LocalDate
import java.util.*
import kotlin.random.Random

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
                    Random.Default.nextLong(111111111, 999999999).toString()
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

class YtelsesperiodeGenerator(
        maxUniqueArbeidsgivere: Int,
        maxUniquePersoner: Int,
        private val initDate: LocalDate = LocalDate.of(2020, 1, 1)
) : Iterable<Ytelsesperiode>, Iterator<Ytelsesperiode>

{

    private val arbeidsgiverGenerator = ArbeidsgiverGenerator(maxUniqueArbeidsgivere)
    private val personGenerator = PersonGenerator(maxUniquePersoner)
    private var numGeneratedPerioder = 0
    private val maxPeriodeLength = 31L;
    private var offset = 0.toLong()

    private fun randomPeriode(): Periode {
        val fom = initDate.plusDays(Random.Default.nextLong(0, (numGeneratedPerioder % 2000 + 1).toLong()))
        val tom = fom.plusDays(Random.Default.nextLong(1, maxPeriodeLength))
        return Periode(fom, tom)
    }

    private val sykemeldingsGrader = listOf(20, 30, 50, 80, 100)

    private fun nextYtelsesperiode() : Ytelsesperiode {
        val periode = randomPeriode()
        numGeneratedPerioder++
        return Ytelsesperiode(
                periode,
                offset++,
                Arbeidsforhold("",
                        personGenerator.getRandomPerson(),
                        arbeidsgiverGenerator.getRandomArbeidsGiver()
                ),
                UUID.randomUUID().toString(),
                BigDecimal(Random.Default.nextLong(100, 10000)),
                periodStateGenerator(),
                BigDecimal(sykemeldingsGrader.pickRandom()),
                BigDecimal(Random.Default.nextLong(10, 1000)),
                periode.tom,
                listOf(Periode(periode.fom, periode.tom.minusDays(2))),
                Ytelsesperiode.Ytelse.SP,
                UUID.randomUUID().toString(),
                LocalDate.now()
        )
    }

    override fun iterator(): Iterator<Ytelsesperiode> {
        return this
    }

    override fun hasNext(): Boolean {
        return true
    }

    override fun next(): Ytelsesperiode {
        return nextYtelsesperiode()
    }
}




private fun <E> List<E>.pickRandom() : E {
    return this[Random.Default.nextInt(0, this.size)]!!
}
