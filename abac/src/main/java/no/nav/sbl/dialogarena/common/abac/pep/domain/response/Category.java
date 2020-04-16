package no.nav.sbl.dialogarena.common.abac.pep.domain.response;

import lombok.EqualsAndHashCode;
import no.nav.sbl.dialogarena.common.abac.pep.domain.Attribute;

@EqualsAndHashCode
public class Category {
    private String categoryId;
    private Attribute attribute;

    public Category() {}

    public Category(String categoryId, Attribute attribute) {
        this.categoryId = categoryId;
        this.attribute = attribute;
    }

    public String getCategoryId() {
        return categoryId;
    }

    public Attribute getAttribute() {
        return attribute;
    }

}

