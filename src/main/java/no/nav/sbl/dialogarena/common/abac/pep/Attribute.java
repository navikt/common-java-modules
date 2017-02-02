package no.nav.sbl.dialogarena.common.abac.pep;

import com.google.gson.annotations.SerializedName;

class Attribute {
    @SerializedName("AttributeId")
    private final String attributeId;
    @SerializedName("Value")
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
