package no.nav.sbl.dialogarena.common.abac.pep;

import no.nav.sbl.dialogarena.common.abac.pep.domain.response.BiasedDecisionResponse;
import no.nav.sbl.dialogarena.common.abac.pep.exception.PepException;

import javax.naming.NamingException;

public interface Pep {
    BiasedDecisionResponse isServiceCallAllowedWithOidcToken(String oidcToken, String domain, String fnr) throws PepException;

    BiasedDecisionResponse isServiceCallAllowedWithIdent(String subjectId, String domain, String fnr) throws PepException;

    BiasedDecisionResponse isSubjectAuthorizedToSeeKode7(String subjectId, String domain) throws PepException;

    BiasedDecisionResponse isSubjectAuthorizedToSeeKode6(String subjectId, String domain) throws PepException;

    BiasedDecisionResponse isSubjectAuthorizedToSeeEgenAnsatt(String subjectId, String domain) throws PepException;

    boolean isSubjectMemberOfModigOppfolging(String subjectId) throws NamingException;
}
