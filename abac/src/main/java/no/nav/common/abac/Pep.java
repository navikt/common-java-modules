package no.nav.common.abac;

import no.nav.common.abac.domain.AbacPersonId;
import no.nav.common.abac.domain.request.ActionId;

public interface Pep {

    /**
     * Sjekker om veileder har tilgang til enhet
     * @param veilederIdent identen til veileder (f.eks Z1234567)
     * @param enhetId enheten som det sjekkes tilgang på
     */
    void sjekkVeiledertilgangTilEnhet(String veilederIdent, String enhetId, RequestInfo requestInfo);

    /**
     *
     * @param veilederIdent identen til veileder (f.eks Z1234567)
     * @param actionId hvilken tilgang spørres det etter
     * @param personId identifikator for personen det sjekkes tilgang på (fnr/aktør id)
     */
    void sjekkVeiledertilgangTilPerson(String veilederIdent, ActionId actionId, AbacPersonId personId, RequestInfo requestInfo);

    /**
     * Sjekk tilgang ved å bruke en innlogget brukers ID token
     * @param innloggetBrukerIdToken OIDC ID token til innlogget bruker. Kan enten være tokenet til en veileder eller ekstern bruker.
     * @param actionId hvilken tilgang spørres det etter
     * @param personId identifikator for personen det sjekkes tilgang på (fnr/aktør id)
     */
    void sjekkTilgangTilPerson(String innloggetBrukerIdToken, ActionId actionId, AbacPersonId personId, RequestInfo requestInfo);

    /**
     * Sjekker om veileder har tilgang til kode 6 brukere
     * @param veilederIdent identen til veileder (f.eks Z1234567)
     */
    void sjekkVeiledertilgangTilKode6(String veilederIdent, RequestInfo requestInfo);

    /**
     * Sjekker om veileder har tilgang til kode 7 brukere
     * @param veilederIdent identen til veileder (f.eks Z1234567)
     */
    void sjekkVeiledertilgangTilKode7(String veilederIdent, RequestInfo requestInfo);

    /**
     * Sjekker om veileder har tilgang til egen ansatt
     * @param veilederIdent identen til veileder (f.eks Z1234567)
     */
    void sjekkVeiledertilgangTilEgenAnsatt(String veilederIdent, RequestInfo requestInfo);

    /**
     * Klienten som blir brukt til å sende forespørsler til abac.
     * @return abac klienten
     */
    AbacClient getAbacClient();

}
