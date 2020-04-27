package no.nav.common.abac;

import no.nav.common.abac.domain.AbacPersonId;

public interface IAbacService {

    void sjekkVeilederTilgangTilEnhet(String veilederIdent, String enhetId);

    void sjekkVeilederTilgangTilBruker(String veilederIdent, AbacPersonId personId);

    AbacClient getAbacClient();

}
