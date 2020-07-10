package no.nav.helse.spion.vedtaksmelding

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.KotlinModule
import no.nav.helse.spion.domene.Arbeidsgiver
import no.nav.helse.spion.domene.ArbeidsgiverGenerator
import no.nav.helse.spion.domene.Periode
import no.nav.helse.spion.domene.ytelsesperiode.PersonGenerator
import java.time.LocalDate
import kotlin.random.Random

private val periodStateGenerator = {
    val rand = Random.Default.nextInt(0, 100)
    when {
        rand < 5 -> VedtaksmeldingsStatus.AVSLÅTT
        rand < 25 -> VedtaksmeldingsStatus.BEHANDLES
        rand < 30 -> VedtaksmeldingsStatus.HENLAGT
        else -> VedtaksmeldingsStatus.INNVILGET
    }
}

class SpleisVedtaksmeldingGenerator(
        private val om: ObjectMapper = ObjectMapper().registerModule(KotlinModule()),
        fixedListArbeidsgivere: MutableList<Arbeidsgiver>? = null,
        maxUniqueArbeidsgivere: Int = 10,
        maxUniquePersoner: Int = 100,
        private val initDate: LocalDate = LocalDate.of(2020, 1, 1)
) : Iterable<SpleisMelding>, Iterator<SpleisMelding> {

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

    private fun generate(): SpleisMelding {
        val periode = randomPeriode()
        val person = personGenerator.getRandomPerson()
        numGeneratedVedtak++

        val vedtak = SpleisVedtakDto(
            periode.fom,
            periode.tom,
                2,
                5,
                listOf(SpleisVedtakDto.SpleisUtbetalingDto(
                        arbeidsgiverGenerator.getRandomArbeidsGiver().arbeidsgiverId!!,
                        "UKJENT",
                        10000,
                        listOf(SpleisVedtakDto.SpleisUtbetalingDto.SpleisUtbetalingslinjeDto(
                                periode.fom,
                                periode.tom,
                                10000/7,
                                10000,
                                1.0,
                                2
                    ))
                )),
                emptyList()
            )

        return SpleisMelding(person.identitetsnummer, numGeneratedVedtak.toLong(), SpleisMeldingstype.Vedtak.name, om.writeValueAsString(vedtak))
    }

    override fun iterator(): Iterator<SpleisMelding> {
        return this
    }

    override fun hasNext(): Boolean {
        return true
    }

    override fun next(): SpleisMelding {
        return generate()
    }
}


private fun <E> List<E>.pickRandom(): E {
    return this[Random.Default.nextInt(0, this.size)]!!
}
