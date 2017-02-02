package no.nav.sbl.dialogarena.common.abac.pep;


import no.nav.sbl.dialogarena.common.abac.pep.xacml.Decision;

public class XacmlResponse {
    private Decision decision;

    Decision getDecision() {
        return decision;
    }

    XacmlResponse withResponse(Decision decision) {
        this.decision = decision;
        return this;
    }
}
