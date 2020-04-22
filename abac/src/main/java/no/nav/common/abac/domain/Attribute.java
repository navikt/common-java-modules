package no.nav.common.abac.domain;

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

    @Override
    public String toString() {
        return "AttributeId: " + attributeId + "Attribute value: " + value;
    }
}
