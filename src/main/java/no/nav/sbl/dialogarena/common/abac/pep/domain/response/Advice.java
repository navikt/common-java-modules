package no.nav.sbl.dialogarena.common.abac.pep.domain.response;

import lombok.EqualsAndHashCode;

@EqualsAndHashCode
public class Advice {
    private String id;
    private AttributeAssignment attributeAssignment;

    public Advice(String id, AttributeAssignment attributeAssignment) {

        this.id = id;
        this.attributeAssignment = attributeAssignment;
    }

    @Override
    public String toString() {
        String attributeString = "Attribute: " + attributeAssignment.toString();
        return "Id: " + id + "\n\t" + attributeString + "\n";
    }

}
