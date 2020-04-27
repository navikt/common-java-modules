package no.nav.common.abac;

import no.nav.common.abac.domain.AbacPersonId;
import no.nav.common.abac.domain.ResourceType;
import no.nav.common.abac.domain.request.ActionId;
import no.nav.common.abac.domain.request.Request;
import no.nav.common.abac.domain.response.BiasedDecisionResponse;
import no.nav.common.abac.exception.PepException;

public interface Pep {

    /**
     *
     * @param oidcToken the body of an oidc token
     * @param domain domain for applikasjon, f eks veilarb
     * @param personId fnr or aktor id of the person
     * @return decision from ABAC together with the full response from ABAC
     * @throws PepException a general exception that indicated that something went wrong in the library
     */
    BiasedDecisionResponse isServiceCallAllowedWithOidcToken(String oidcToken, String domain, AbacPersonId personId) throws PepException;

    /**
     *
     * @param subjectId ident og saksbehandler
     * @param domain domain for applikasjon, f eks veilarb
     * @param personId fnr or aktor id of the person
     * @return decision from ABAC together with the full response from ABAC
     * @throws PepException a general exception that indicated that something went wrong in the library
     */
    BiasedDecisionResponse isServiceCallAllowedWithIdent(String subjectId, String domain, AbacPersonId personId) throws PepException;

    BiasedDecisionResponse isSubjectAuthorizedToSeeKode7(String oidcToken, String domain) throws PepException;

    BiasedDecisionResponse isSubjectAuthorizedToSeeKode6(String oidcToken, String domain) throws PepException;

    BiasedDecisionResponse isSubjectAuthorizedToSeeEgenAnsatt(String oidcToken, String domain) throws PepException;

    BiasedDecisionResponse isSubjectMemberOfModiaOppfolging(String oidcToken, String domain) throws PepException;

    RequestData nyRequest() throws PepException;

    BiasedDecisionResponse harTilgang(RequestData requestData) throws PepException;
    BiasedDecisionResponse harTilgang(Request request) throws PepException;

    BiasedDecisionResponse harTilgangTilEnhet(String enhet, String systembruker, String domain) throws PepException;

    BiasedDecisionResponse harInnloggetBrukerTilgangTilPerson(String fnr, String domain) throws PepException;

    BiasedDecisionResponse harInnloggetBrukerTilgangTilPerson(AbacPersonId personId, String domain, ActionId action, ResourceType resourceType) throws PepException;

    void ping() throws PepException;
}
