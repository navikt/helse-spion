package no.nav.helse.spion.domenetjenester

import no.nav.helse.spion.domene.ytelsesperiode.Ytelsesperiode
import no.nav.helse.spion.domene.ytelsesperiode.repository.YtelsesperiodeRepository
import no.nav.helse.spion.auth.AuthorizationsRepository
import no.nav.helse.spion.domene.AltinnOrganisasjon

class SpionService(private val sakRepo: YtelsesperiodeRepository, private val authRepo: AuthorizationsRepository) {

    fun hentYtelserForPerson(identitetsnummer: String, virksomhetsnummer: String, arbeidsgiverIdentitetsnummer: String?): List<Ytelsesperiode> {
        return sakRepo.hentYtelserForPerson(identitetsnummer, virksomhetsnummer)
    }
    fun hentArbeidsgivere(identitet: String) : Set<AltinnOrganisasjon> {
        return authRepo.hentOrgMedRettigheterForPerson(identitet)
    }
}