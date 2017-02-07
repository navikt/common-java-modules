package no.nav.sbl.dialogarena.common.abac.pep;

class Attribute {
    private final String attributeId;
    private final String value;

    Attribute(String attributeId, String value) {

        this.attributeId = attributeId;
        this.value = value;
    }

    public String getAttributeId() {
        return attributeId;
    }

    public String getValue() {
        return value;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Attribute attribute = (Attribute) o;

        if (attributeId != null ? !attributeId.equals(attribute.attributeId) : attribute.attributeId != null)
            return false;
        return value != null ? value.equals(attribute.value) : attribute.value == null;
    }

    @Override
    public int hashCode() {
        int result = attributeId != null ? attributeId.hashCode() : 0;
        result = 31 * result + (value != null ? value.hashCode() : 0);
        return result;
    }
}
