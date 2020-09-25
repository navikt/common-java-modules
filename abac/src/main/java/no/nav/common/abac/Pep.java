package no.nav.common.abac;

import no.nav.common.abac.domain.request.ActionId;
import no.nav.common.types.identer.EksternBrukerId;
import no.nav.common.types.identer.EnhetId;
import no.nav.common.types.identer.NavIdent;

public interface Pep {

    /**
     * Sjekker om veileder har tilgang til enhet
     *
     * @param veilederIdent identen til veileder (f.eks Z1234567)
     * @param enhetId       enheten som det sjekkes tilgang på
     * @return true hvis tilgang, false hvis ikke tilgang
     */
    boolean harVeilederTilgangTilEnhet(NavIdent veilederIdent, EnhetId enhetId);

    /**
     * Sjekk tilgang til enhet ved å bruke en innlogget brukers ID token
     *
     * @param innloggetBrukerIdToken OIDC ID token til innlogget bruker. Kan enten være tokenet til en veileder eller ekstern bruker.
     * @param enhetId       enheten som det sjekkes tilgang på
     * @return true hvis tilgang, false hvis ikke tilgang
     */
    boolean harTilgangTilEnhet(String innloggetBrukerIdToken, EnhetId enhetId);

    /**
     * Sjekk tilgang til enhet med sperre ved å bruke en innlogget brukers ID token.
     * Brukes for å sjekke tilgang til KVP. Eksterne brukere vil alltid få permit for alle kontorer.
     *
     * @param innloggetBrukerIdToken OIDC ID token til innlogget bruker. Kan enten være tokenet til en veileder eller ekstern bruker.
     * @param enhetId       enheten som det sjekkes tilgang på
     * @return true hvis tilgang, false hvis ikke tilgang
     */
    boolean harTilgangTilEnhetMedSperre(String innloggetBrukerIdToken, EnhetId enhetId);


    /**
     * @param veilederIdent     identen til veileder (f.eks Z1234567)
     * @param actionId          hvilken tilgang spørres det etter
     * @param eksternBrukerId   fødselsnummer eller aktørId for personen det sjekkes tilgang på
     * @return true hvis tilgang, false hvis ikke tilgang
     */
    boolean harVeilederTilgangTilPerson(NavIdent veilederIdent, ActionId actionId, EksternBrukerId eksternBrukerId);

    /**
     * Sjekk tilgang ved å bruke en innlogget brukers ID token
     *
     * @param innloggetBrukerIdToken    OIDC ID token til innlogget bruker. Kan enten være tokenet til en veileder eller ekstern bruker.
     * @param actionId                  hvilken tilgang spørres det etter
     * @param eksternBrukerId           fødselsnummer eller aktørId for personen det sjekkes tilgang på
     * @return true hvis tilgang, false hvis ikke tilgang
     */
    boolean harTilgangTilPerson(String innloggetBrukerIdToken, ActionId actionId, EksternBrukerId eksternBrukerId);

    /**
     * Sjekker om bruker har tilgang til domenet oppfølging ("veilarb")
     *
     * @param innloggetBrukerIdToken OIDC ID token til en innlogget bruker (systembruker eller saksbehandler)
     * @return true hvis tilgang, false hvis ikke tilgang
     */
    boolean harTilgangTilOppfolging(String innloggetBrukerIdToken);

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
    boolean harVeilederTilgangTilKode6(NavIdent veilederIdent);

    /**
     * Sjekker om veileder har tilgang til kode 7 brukere
     *
     * @param veilederIdent identen til veileder (f.eks Z1234567)
     * @return true hvis tilgang, false hvis ikke tilgang
     */
    boolean harVeilederTilgangTilKode7(NavIdent veilederIdent);

    /**
     * Sjekker om veileder har tilgang til egen ansatt
     *
     * @param veilederIdent identen til veileder (f.eks Z1234567)
     * @return true hvis tilgang, false hvis ikke tilgang
     */
    boolean harVeilederTilgangTilEgenAnsatt(NavIdent veilederIdent);

    /**
     * Klienten som blir brukt til å sende forespørsler til abac.
     *
     * @return abac klienten
     */
    AbacClient getAbacClient();

}
