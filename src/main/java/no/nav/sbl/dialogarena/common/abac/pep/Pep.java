package no.nav.sbl.dialogarena.common.abac.pep;

import no.nav.sbl.dialogarena.common.abac.pep.domain.response.BiasedDecisionResponse;
import no.nav.sbl.dialogarena.common.abac.pep.exception.PepException;

import javax.naming.NamingException;

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

    BiasedDecisionResponse isSubjectAuthorizedToSeeKode7(String subjectId, String domain) throws PepException;

    BiasedDecisionResponse isSubjectAuthorizedToSeeKode6(String subjectId, String domain) throws PepException;

    BiasedDecisionResponse isSubjectAuthorizedToSeeEgenAnsatt(String subjectId, String domain) throws PepException;

    BiasedDecisionResponse isSubjectMemberOfModiaOppfolging(String ident) throws PepException;
}
