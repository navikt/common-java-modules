package no.nav.sbl.dialogarena.common.abac.pep.domain.response;

import lombok.EqualsAndHashCode;
import no.nav.sbl.dialogarena.common.abac.pep.domain.Attribute;

@EqualsAndHashCode(callSuper = true)
public class AttributeAssignment extends Attribute {
    public AttributeAssignment(String attributeId, String value) {
        super(attributeId, value);
    }
}
