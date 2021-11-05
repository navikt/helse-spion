package no.nav.helse.spion.auth

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import com.github.javafaker.Faker
import no.nav.helse.spion.domene.AltinnOrganisasjon
import no.nav.helse.spion.domene.ytelsesperiode.Ytelsesperiode
import java.time.LocalDateTime
import javax.sql.DataSource

/**
 * Henter et tilfeldig sett arbeidsgivere fra perioder i databasen
 * og lager en ACL på bakgrunn av denne. Dette gjør at hvilkensomhelst identitet som logger inn via OIDC-stuben i dev
 * vil få tilganger til noen arbeidsgivere som har perioder i databasen
 */
class DynamicMockAuthRepo(private val om: ObjectMapper, private val dataSource: DataSource) : AuthorizationsRepository {
    val cache = mutableMapOf<String, Pair<LocalDateTime, Set<AltinnOrganisasjon>>>()

    override fun hentOrgMedRettigheterForPerson(identitetsnummer: String): Set<AltinnOrganisasjon> {
        val hasValidCacheEntry = cache[identitetsnummer]?.first?.isAfter(LocalDateTime.now()) ?: false
        if (hasValidCacheEntry) {
            return cache[identitetsnummer]!!.second
        }

        dataSource.connection.use { connection ->
            val res = connection.prepareStatement(
                "select * from ytelsesperiode where random() < 0.5 limit 100"
            )
                .executeQuery()

            val resultatListe = mutableListOf<Ytelsesperiode>()

            while (res.next()) {
                resultatListe.add(
                    om.readValue(
                        res.getString("data")
                    )
                )
            }

            val nameFaker = Faker()

            val acl = resultatListe
                .distinctBy { it.arbeidsforhold.arbeidsgiver.arbeidsgiverId }
                .zipWithNext().flatMap {
                    val juridisk = it.first.arbeidsforhold.arbeidsgiver
                    val virksomhet = it.second.arbeidsforhold.arbeidsgiver
                    val name = nameFaker.funnyName().name()
                    listOf(
                        AltinnOrganisasjon("$name AS", organizationNumber = juridisk.arbeidsgiverId, type = "Enterprise", organizationForm = "AS"),
                        AltinnOrganisasjon("$name Øst", organizationNumber = virksomhet.arbeidsgiverId, parentOrganizationNumber = juridisk.arbeidsgiverId, type = "Business", organizationForm = "BEDR")
                    )
                }.toSet()

            if (cache.keys.size > 20) cache.clear()

            cache[identitetsnummer] = Pair(LocalDateTime.now().plusHours(24), acl)
            return acl
        }
    }
}
