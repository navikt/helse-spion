package no.nav.helse.spion.integrasjon.pdl

import me.lazmaid.kraph.Kraph

val hentNavn = Kraph {
    query("hentPerson") {
        field("fornavn")
        field("mellomnavn")
        field("etternavn")
        field("mellomnavn")
    }
}