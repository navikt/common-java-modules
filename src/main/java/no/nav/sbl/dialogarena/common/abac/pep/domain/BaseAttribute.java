package no.nav.sbl.dialogarena.common.abac.pep.domain;

import java.util.ArrayList;
import java.util.List;

public class BaseAttribute {

    private List<Attribute> attribute;

    public List<Attribute> getAttribute() {
        if (attribute == null)
            attribute = new ArrayList<>();
        return attribute;
    }
}
