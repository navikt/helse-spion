package no.nav.helse.spion.vedtaksmelding

import no.nav.helse.spion.domene.Arbeidsgiver
import no.nav.helse.spion.domene.ArbeidsgiverGenerator
import no.nav.helse.spion.domene.Periode
import no.nav.helse.spion.domene.ytelsesperiode.PersonGenerator
import java.time.LocalDate
import kotlin.random.Random
import kotlin.random.Random.Default.nextDouble

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
