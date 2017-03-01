package no.nav.sbl.dialogarena.common.abac.pep.domain;

import lombok.EqualsAndHashCode;

@EqualsAndHashCode
public class Attribute {
    public String getAttributeId() {
        return attributeId;
    }

    private final String attributeId;

    public String getValue() {
        return value;
    }

    private final String value;

    public Attribute(String attributeId, String value) {

        this.attributeId = attributeId;
        this.value = value;
    }

}
