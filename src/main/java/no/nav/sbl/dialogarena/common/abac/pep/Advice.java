package no.nav.sbl.dialogarena.common.abac.pep;

public class Advice {
    private String id;
    private AttributeAssignment attributeAssignment;

    public Advice(String id, AttributeAssignment attributeAssignment) {

        this.id = id;
        this.attributeAssignment = attributeAssignment;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Advice)) return false;

        Advice advice = (Advice) o;

        if (id != null ? !id.equals(advice.id) : advice.id != null) return false;
        return attributeAssignment != null ? attributeAssignment.equals(advice.attributeAssignment) : advice.attributeAssignment == null;

    }

    @Override
    public int hashCode() {
        int result = id != null ? id.hashCode() : 0;
        result = 31 * result + (attributeAssignment != null ? attributeAssignment.hashCode() : 0);
        return result;
    }
}
