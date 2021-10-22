package no.nav.helse.spion.domene

data class Arbeidsgiver(

    /**
     * Organisasjonsnummer for virksomheten hvis bedrift, eller fødselsnummer på oppdragsgiver hvis privat
     */
    val arbeidsgiverId: String
)
