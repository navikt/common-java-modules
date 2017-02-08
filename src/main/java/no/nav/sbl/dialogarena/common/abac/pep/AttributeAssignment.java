package no.nav.sbl.dialogarena.common.abac.pep;

import lombok.EqualsAndHashCode;

@EqualsAndHashCode(callSuper = true)
public class AttributeAssignment extends Attribute {
    public AttributeAssignment(String attributeId, String value) {
        super(attributeId, value);
    }
}
