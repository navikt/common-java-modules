package no.nav.sbl.dialogarena.common.abac.pep;

public class AbacPersonId {

    private String id;
    private Type type;

    private AbacPersonId(String id, Type type) {
        this.id = id;
        this.type = type;
    }

    private enum Type {
        FNR,
        AKTOR_ID
    }

    public static AbacPersonId fnr(String fnr) {
        return new AbacPersonId(fnr, Type.FNR);
    }

    public static AbacPersonId aktorId(String aktorId) {
        return new AbacPersonId(aktorId, Type.AKTOR_ID);
    }

    public boolean isFnr() {
        return Type.FNR.equals(type);
    }

    public boolean isAktorId() {
        return Type.AKTOR_ID.equals(type);
    }

    public String getId() {
        return id;
    }
}
