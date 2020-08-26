package no.nav.helse.utils

fun String.loadFromResources() : String {
    return ClassLoader.getSystemResource(this).readText()
}