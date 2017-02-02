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

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        XacmlResponse response = (XacmlResponse) o;

        return decision == response.decision;

    }

    @Override
    public int hashCode() {
        return decision != null ? decision.hashCode() : 0;
    }
}
