package no.nav.helse.spion.domene.ytelsesperiode

import com.github.javafaker.Faker
import no.nav.helse.spion.domene.Person
import no.nav.helse.spion.web.dto.validation.FoedselsNrValidator
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import kotlin.random.Random

class PersonGenerator(private val maxUniquePersons: Int = 1000) {
    private val faker = Faker()
    private val personer = mutableListOf<Person>()
    fun getRandomPerson(): Person {
        return if (personer.size >= maxUniquePersons) {
            personer.pickRandom()
        } else {
            val tenYearsBeforeAfterEpoch = 3600 * 24 * 365 * 10L
            val birthdate = LocalDate.ofInstant(Instant.ofEpochSecond(Random.nextLong(-tenYearsBeforeAfterEpoch, tenYearsBeforeAfterEpoch)), ZoneId.systemDefault())

            val bday = birthdate.format(DateTimeFormatter.ofPattern("ddMMyy"))
            var pnr = Random.nextInt(100, 999)
            var knr1 = FoedselsNrValidator.checksum(FoedselsNrValidator.Companion.tabeller.kontrollsiffer1, bday + pnr)
            var knr2 = FoedselsNrValidator.checksum(FoedselsNrValidator.Companion.tabeller.kontrollsiffer2, bday + pnr + knr1)

            // visse kombinasjoner av tall kan gi kontrollsiffer 10 (og da et 12-sifret nummer), disse blir forkastet
            while (knr1 == 10 || knr2 == 10) {
                pnr = Random.nextInt(100, 999)
                knr1 = FoedselsNrValidator.checksum(FoedselsNrValidator.Companion.tabeller.kontrollsiffer1, bday + pnr)
                knr2 = FoedselsNrValidator.checksum(FoedselsNrValidator.Companion.tabeller.kontrollsiffer2, bday + pnr + knr1)
            }


            val fnr = bday + pnr + knr1 + knr2

            val person = Person(
                    faker.name().firstName(),
                    faker.name().lastName(),
                    fnr
            )
            personer.add(person)
            person
        }
    }
}
private fun <E> List<E>.pickRandom(): E {
    return this[Random.Default.nextInt(0, this.size)]!!
}