package no.nav.common.abac.domain;

import lombok.EqualsAndHashCode;

@EqualsAndHashCode
public class Attribute {

    private final String attributeId;

    private final String value;

    // If set to true in a request, then the response will include a Category with the same attributeId and value
    private final Boolean includeInResult;

    public Attribute(String attributeId, String value, Boolean includeInResult) {
        this.attributeId = attributeId;
        this.value = value;
        this.includeInResult = includeInResult;
    }

    public Attribute(String attributeId, String value) {
        this(attributeId, value, null);
    }

    public String getAttributeId() {
        return attributeId;
    }

    public String getValue() {
        return value;
    }

    public Boolean getIncludeInResult() {
        return includeInResult;
    }

    @Override
    public String toString() {
        return "AttributeId: " + attributeId + "Attribute value: " + value;
    }

}
