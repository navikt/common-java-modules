package no.nav.sbl.dialogarena.common.abac.pep.domain.response;

import lombok.EqualsAndHashCode;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@EqualsAndHashCode
public class Response {
    private Decision decision;
    private List<Advice> associatedAdvice;
    private List<Category> category;

    public Decision getDecision() {
        return decision;
    }

    public List<Advice> getAssociatedAdvice() {
        if (associatedAdvice == null) {
            associatedAdvice = new ArrayList<>();
        }
        return associatedAdvice;
    }

    public List<Category> getCategory() {
        if (category == null) {
            category = new ArrayList<>();
        }
        return category;
    }

    public Response withDecision(Decision decision) {
        this.decision = decision;
        return this;
    }

    public Response withAssociatedAdvice(List<Advice> associatedAdvice) {
        this.associatedAdvice = associatedAdvice;
        return this;
    }

    public Response withCategories(List<Category> category) {
        this.category = category;
        return this;
    }

    public Response withCategory(Category category) {
        return withCategories(Collections.singletonList(category));
    }

}
