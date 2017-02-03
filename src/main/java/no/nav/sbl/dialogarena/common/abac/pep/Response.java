package no.nav.sbl.dialogarena.common.abac.pep;

import no.nav.sbl.dialogarena.common.abac.pep.xacml.Decision;


public class Response {
    private Decision decision;

    Decision getDecision() {
        return decision;
    }

    public Response withDecision(Decision decision) {
        this.decision = decision;
        return this;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Response response = (Response) o;

        return decision == response.decision;

    }

    @Override
    public int hashCode() {
        return decision != null ? decision.hashCode() : 0;
    }
}
