package no.nav.sbl.dialogarena.common.abac.pep.domain;

import lombok.EqualsAndHashCode;

@EqualsAndHashCode
public class Attribute {
    private final String attributeId;
    private final String value;

    public Attribute(String attributeId, String value) {

        this.attributeId = attributeId;
        this.value = value;
    }

}
