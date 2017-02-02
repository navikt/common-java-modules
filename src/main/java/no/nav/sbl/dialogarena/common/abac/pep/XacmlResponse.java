package no.nav.sbl.dialogarena.common.abac.pep;


import no.nav.sbl.dialogarena.common.abac.pep.xacml.Decision;

public class XacmlResponse {
    private Decision decision;

    public Decision getDecision() {
        return decision;
    }

    public XacmlResponse withDecision(Decision decision) {
        this.decision = decision;
        return this;
    }
}
