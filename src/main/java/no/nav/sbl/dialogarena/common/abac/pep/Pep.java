package no.nav.sbl.dialogarena.common.abac.pep;

import no.nav.sbl.dialogarena.common.abac.pep.domain.response.BiasedDecisionResponse;
import no.nav.sbl.dialogarena.common.abac.pep.exception.PepException;

public interface Pep {
    BiasedDecisionResponse isServiceCallAllowed(String oidcToken, String subjectId, String domain, String fnr) throws PepException;
}
