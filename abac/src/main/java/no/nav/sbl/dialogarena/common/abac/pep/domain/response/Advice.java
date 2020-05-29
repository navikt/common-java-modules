package no.nav.sbl.dialogarena.common.abac.pep.domain.response;

import lombok.EqualsAndHashCode;
import lombok.Getter;

import java.util.List;

@EqualsAndHashCode
@Getter
public class Advice {

    private final String id;
    private final List<AttributeAssignment> attributeAssignment;

    public Advice(String id, List<AttributeAssignment> attributeAssignment) {

        this.id = id;
        this.attributeAssignment = attributeAssignment;
    }

    @Override
    public String toString() {
        String attributeString = "Attribute: " + attributeAssignment.toString();
        return "Id: " + id + "\n\t" + attributeString + "\n";
    }

}
