package no.nav.common.abac.domain.response;

import lombok.EqualsAndHashCode;
import no.nav.common.abac.domain.Attribute;

@EqualsAndHashCode(callSuper = true)
public class AttributeAssignment extends Attribute {
    public AttributeAssignment(String attributeId, String value) {
        super(attributeId, value);
    }
}
