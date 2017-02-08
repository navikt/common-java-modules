package no.nav.sbl.dialogarena.common.abac.pep;

import no.nav.sbl.dialogarena.common.abac.pep.xacml.Decision;

import java.util.List;


public class Response {
    private Decision decision;
    private List<Advice> associatedAdvice;

    public Decision getDecision() {
        return decision;
    }

    public Response withDecision(Decision decision) {
        this.decision = decision;
        return this;
    }

    public Response withAssociatedAdvice(List<Advice> associatedAdvice) {
        this.associatedAdvice = associatedAdvice;
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
