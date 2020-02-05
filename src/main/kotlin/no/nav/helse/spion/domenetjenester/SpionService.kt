package no.nav.helse.spion.domenetjenester

import no.nav.helse.spion.domene.Arbeidsgiver
import no.nav.helse.spion.domene.ytelsesperiode.Ytelsesperiode
import no.nav.helse.spion.domene.ytelsesperiode.repository.YtelsesperiodeRepository
import no.nav.helse.spion.auth.AuthorizationsRepository
import no.nav.helse.spion.domene.AltinnOrganisasjon
import no.nav.helse.spion.domene.sak.Sak
import no.nav.helse.spion.domene.sak.repository.SaksinformasjonRepository

class SpionService(private val sakRepo: YtelsesperiodeRepository, private val authRepo: AuthorizationsRepository) {

    fun hentYtelserForPerson(identitetsnummer: String, arbeidsgiverOrgnummer: String?, arbeidsgiverIdentitetsnummer: String?): List<Ytelsesperiode> {
        return sakRepo.hentYtelserForPerson(identitetsnummer, orgnr)
    }
    fun hentArbeidsgivere(identitet: String) : Set<AltinnOrganisasjon> {
        return authRepo.hentOrgMedRettigheterForPerson(identitet)
    }
}