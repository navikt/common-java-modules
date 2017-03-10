package no.nav.sbl.dialogarena.common.abac.pep;

import no.nav.sbl.dialogarena.common.abac.pep.domain.response.BiasedDecisionResponse;

public interface Pep {
    BiasedDecisionResponse isServiceCallAllowedWithToken(String oidcToken, String domain, String fnr);

    BiasedDecisionResponse isServiceCallAllowedWithIdent(String subjectId, String domain, String fnr);
}
