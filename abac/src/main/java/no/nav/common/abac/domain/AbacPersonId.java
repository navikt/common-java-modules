package no.nav.common.abac.domain;

import java.util.Objects;

public class AbacPersonId {

    private String id;
    private Type type;

    private AbacPersonId(String id, Type type) {
        this.id = id;
        this.type = type;
    }

    public enum Type {
        FNR,
        AKTOR_ID
    }

    public static AbacPersonId fnr(String fnr) {
        return new AbacPersonId(fnr, Type.FNR);
    }

    public static AbacPersonId aktorId(String aktorId) {
        return new AbacPersonId(aktorId, Type.AKTOR_ID);
    }

    public String getId() {
        return id;
    }

    public Type getType() {
        return type;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        AbacPersonId that = (AbacPersonId) o;
        return Objects.equals(id, that.id) &&
                type == that.type;
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, type);
    }
}
