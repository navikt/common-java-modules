package no.nav.sbl.dialogarena.common.abac.pep;

import lombok.EqualsAndHashCode;
import no.nav.sbl.dialogarena.common.abac.pep.xacml.Decision;

import java.util.List;

@EqualsAndHashCode
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

}
