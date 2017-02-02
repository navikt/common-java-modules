package no.nav.sbl.dialogarena.common.abac.pep;

import java.util.ArrayList;
import java.util.List;

class BaseAttribute {

    private List<Attribute> attribute;

    List<Attribute> getAttribute() {
        if (attribute == null)
            attribute = new ArrayList<>();
        return attribute;
    }
}
