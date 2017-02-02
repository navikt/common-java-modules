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
}
