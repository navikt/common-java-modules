package no.nav.common.abac;

import no.nav.common.abac.domain.AbacPersonId;
import no.nav.common.abac.domain.request.ActionId;

public interface Pep {

    /**
     * Sjekker om veileder har tilgang til enhet
     *
     * @param veilederIdent identen til veileder (f.eks Z1234567)
     * @param enhetId       enheten som det sjekkes tilgang på
     * @return true hvis tilgang, false hvis ikke tilgang
     */
    boolean harVeilederTilgangTilEnhet(String veilederIdent, String enhetId);

    /**
     * Sjekk tilgang til enhet ved å bruke en innlogget brukers ID token
     *
     * @param innloggetBrukerIdToken OIDC ID token til innlogget bruker. Kan enten være tokenet til en veileder eller ekstern bruker.
     * @param enhetId       enheten som det sjekkes tilgang på
     * @return true hvis tilgang, false hvis ikke tilgang
     */
    boolean harTilgangTilEnhet(String innloggetBrukerIdToken, String enhetId);

    /**
     * @param veilederIdent identen til veileder (f.eks Z1234567)
     * @param actionId      hvilken tilgang spørres det etter
     * @param personId      identifikator for personen det sjekkes tilgang på (fnr/aktør id)
     * @return true hvis tilgang, false hvis ikke tilgang
     */
    boolean harVeilederTilgangTilPerson(String veilederIdent, ActionId actionId, AbacPersonId personId);

    /**
     * Sjekk tilgang ved å bruke en innlogget brukers ID token
     *
     * @param innloggetBrukerIdToken OIDC ID token til innlogget bruker. Kan enten være tokenet til en veileder eller ekstern bruker.
     * @param actionId               hvilken tilgang spørres det etter
     * @param personId               identifikator for personen det sjekkes tilgang på (fnr/aktør id)
     * @return true hvis tilgang, false hvis ikke tilgang
     */
    boolean harTilgangTilPerson(String innloggetBrukerIdToken, ActionId actionId, AbacPersonId personId);

    /**
     * Sjekker om veileder har tilgang til domenet oppfølging ("veilarb")
     *
     * @param innloggetVeilederIdToken OIDC ID token til en innlogget veileder
     * @return true hvis tilgang, false hvis ikke tilgang
     */
    boolean harVeilederTilgangTilOppfolging(String innloggetVeilederIdToken);

    /**
     * Sjekker om veileder har tilgang til modia
     *
     * @param innloggetVeilederIdToken OIDC ID token til en innlogget veileder
     * @return true hvis tilgang, false hvis ikke tilgang
     */
    boolean harVeilederTilgangTilModia(String innloggetVeilederIdToken);

    /**
     * Sjekker om veileder har tilgang til kode 6 brukere
     *
     * @param veilederIdent identen til veileder (f.eks Z1234567)
     * @return true hvis tilgang, false hvis ikke tilgang
     */
    boolean harVeilederTilgangTilKode6(String veilederIdent);

    /**
     * Sjekker om veileder har tilgang til kode 7 brukere
     *
     * @param veilederIdent identen til veileder (f.eks Z1234567)
     * @return true hvis tilgang, false hvis ikke tilgang
     */
    boolean harVeilederTilgangTilKode7(String veilederIdent);

    /**
     * Sjekker om veileder har tilgang til egen ansatt
     *
     * @param veilederIdent identen til veileder (f.eks Z1234567)
     * @return true hvis tilgang, false hvis ikke tilgang
     */
    boolean harVeilederTilgangTilEgenAnsatt(String veilederIdent);

    /**
     * Klienten som blir brukt til å sende forespørsler til abac.
     *
     * @return abac klienten
     */
    AbacClient getAbacClient();

}
