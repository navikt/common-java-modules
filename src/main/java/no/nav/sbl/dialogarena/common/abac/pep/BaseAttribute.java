package no.nav.sbl.dialogarena.common.abac.pep;

import com.google.gson.annotations.SerializedName;

import java.util.ArrayList;
import java.util.List;

class BaseAttribute {

    @SerializedName("Attribute")
    private List<Attribute> attribute;

    List<Attribute> getAttribute() {
        if (attribute == null)
            attribute = new ArrayList<>();
        return attribute;
    }
}
