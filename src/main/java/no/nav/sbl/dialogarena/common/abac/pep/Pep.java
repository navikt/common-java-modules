package no.nav.sbl.dialogarena.common.abac.pep;

import no.nav.sbl.dialogarena.common.abac.pep.domain.ResourceType;
import no.nav.sbl.dialogarena.common.abac.pep.domain.request.Action;
import no.nav.sbl.dialogarena.common.abac.pep.domain.request.Request;
import no.nav.sbl.dialogarena.common.abac.pep.domain.response.BiasedDecisionResponse;
import no.nav.sbl.dialogarena.common.abac.pep.exception.PepException;

public interface Pep {

    /**
     *
     * @param oidcToken the body of an oidc token
     * @param domain domain for applikasjon, f eks veilarb
     * @param fnr fnr of the person
     * @return decision from ABAC together with the full response from ABAC
     * @throws PepException a general exception that indicated that something went wrong in the library
     */
    BiasedDecisionResponse isServiceCallAllowedWithOidcToken(String oidcToken, String domain, String fnr) throws PepException;

    /**
     *
     * @param subjectId ident og saksbehandler
     * @param domain domain for applikasjon, f eks veilarb
     * @param fnr fnr of the person
     * @return decision from ABAC together with the full response from ABAC
     * @throws PepException a general exception that indicated that something went wrong in the library
     */
    BiasedDecisionResponse isServiceCallAllowedWithIdent(String subjectId, String domain, String fnr) throws PepException;

    BiasedDecisionResponse isSubjectAuthorizedToSeeKode7(String oidcToken, String domain) throws PepException;

    BiasedDecisionResponse isSubjectAuthorizedToSeeKode6(String oidcToken, String domain) throws PepException;

    BiasedDecisionResponse isSubjectAuthorizedToSeeEgenAnsatt(String oidcToken, String domain) throws PepException;

    BiasedDecisionResponse isSubjectMemberOfModiaOppfolging(String oidcToken, String domain) throws PepException;

    BiasedDecisionResponse harTilgang(Request request) throws PepException;

    BiasedDecisionResponse harInnloggetBrukerTilgangTilPerson(String fnr, String domain) throws PepException;

    BiasedDecisionResponse harInnloggetBrukerTilgangTilPerson(String fnr, String domain, Action.ActionId action, ResourceType resourceType) throws PepException;

    void ping() throws PepException;
}
