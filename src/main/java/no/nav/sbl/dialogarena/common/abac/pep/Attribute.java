package no.nav.sbl.dialogarena.common.abac.pep;

import lombok.EqualsAndHashCode;

@EqualsAndHashCode
class Attribute {
    private final String attributeId;
    private final String value;

    Attribute(String attributeId, String value) {

        this.attributeId = attributeId;
        this.value = value;
    }

}
