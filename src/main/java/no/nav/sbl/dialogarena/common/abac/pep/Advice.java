package no.nav.sbl.dialogarena.common.abac.pep;

import lombok.EqualsAndHashCode;

@EqualsAndHashCode
public class Advice {
    private String id;
    private AttributeAssignment attributeAssignment;

    public Advice(String id, AttributeAssignment attributeAssignment) {

        this.id = id;
        this.attributeAssignment = attributeAssignment;
    }

}
