package no.nav.sbl.dialogarena.common.abac.pep;

import no.nav.sbl.dialogarena.common.abac.pep.domain.response.BiasedDecisionResponse;
import no.nav.sbl.dialogarena.common.abac.pep.exception.PepException;

public interface Pep {
    BiasedDecisionResponseisServiceCallAllowedWithToken(String oidcToken, String domain, String fnr) throws PepException;

    BiasedDecisionResponse isServiceCallAllowedWithIdent(String subjectId, String domain, String fnr)(String oidcToken, String subjectId, String domain, String fnr) throws PepException;

}
