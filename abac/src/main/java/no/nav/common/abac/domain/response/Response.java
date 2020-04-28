package no.nav.common.abac.domain.response;

import lombok.EqualsAndHashCode;

import java.util.ArrayList;
import java.util.List;

@EqualsAndHashCode
public class Response {
    private Decision decision;
    private List<Advice> associatedAdvice;

    public Decision getDecision() {
        return decision;
    }

    public List<Advice> getAssociatedAdvice() {
        if (associatedAdvice == null) {
            associatedAdvice = new ArrayList<>();
        }
        return associatedAdvice;
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