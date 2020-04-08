package no.nav.helse.spion.varsling

import java.time.LocalDate
import java.time.temporal.WeekFields
import java.util.*

/**
 * Interface for å lage en periode-identifikator av en dato
 */
interface VarslingsPeriodeStrategy {

    /**
     * Tar en dato og genererer en periode-id i en streng. Samme dato skal alltid gi samme periode-id
     * gitt samme implementasjon
     */
    fun toPeriodeId(date: LocalDate): String

    /**
     * Gitt en dato skal denne funksjonen kunne gi perioden før perioden
     * den angitte datoen tilhører
     */
    fun previousPeriodeId(date: LocalDate): String
}

class DailyVarslingStrategy() : VarslingsPeriodeStrategy {
    override fun toPeriodeId(date: LocalDate): String {
        return "D-$date"
    }

    override fun previousPeriodeId(date: LocalDate): String {
        return toPeriodeId(date.minusDays(1))
    }
}

class WeeklyVarslingStrategy() : VarslingsPeriodeStrategy {
    override fun toPeriodeId(date: LocalDate): String {
        val weekOfYear = date.get(WeekFields.ISO.weekOfWeekBasedYear())
        return "W-${date.year}-$weekOfYear"
    }

    override fun previousPeriodeId(date: LocalDate): String {
        return toPeriodeId(date.minusWeeks(1))
    }
}

class MontlyVarslingStrategy() : VarslingsPeriodeStrategy {
    override fun toPeriodeId(date: LocalDate): String {
        val weekRules = WeekFields.of(Locale.getDefault())
        val weekOfYear = date.get(weekRules.weekOfWeekBasedYear())
        return "M-${date.year}-${date.monthValue}"
    }

    override fun previousPeriodeId(date: LocalDate): String {
        return toPeriodeId(date.minusMonths(1))
    }
}