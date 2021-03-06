package no.nav.common.abac.domain;

import lombok.ToString;

import java.util.ArrayList;
import java.util.List;

@ToString
public class BaseAttribute {

    private List<Attribute> attribute;

    public List<Attribute> getAttribute() {
        if (attribute == null)
            attribute = new ArrayList<>();
        return attribute;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof BaseAttribute)) return false;

        BaseAttribute that = (BaseAttribute) o;

        return attribute != null ? attribute.equals(that.attribute) : that.attribute == null;
    }

    public void addAttribute(Attribute attribute) {
        getAttribute().add(attribute);
    }

    @Override
    public int hashCode() {
        return attribute != null ? attribute.hashCode() : 0;
    }
}
