package no.nav.sbl.dialogarena.common.abac.pep.domain.response;

import lombok.EqualsAndHashCode;

@EqualsAndHashCode
public class Advice {
    private final String id;
    private final AttributeAssignment attributeAssignment;

    public Advice(String id, AttributeAssignment attributeAssignment) {

        this.id = id;
        this.attributeAssignment = attributeAssignment;
    }

}
