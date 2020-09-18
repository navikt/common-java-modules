package no.nav.common.types.identer;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

public class Id {

    private final String id;

    @JsonCreator
    public Id(String id) {
        if (id == null) {
            throw new IllegalArgumentException("Id cannot be null");
        }

        this.id = id;
    }

    public String get() {
        return id;
    }

    @JsonValue
    @Override
    public String toString() {
        return id;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) {
            return true;
        }

        if (!(obj instanceof Id)) {
            return false;
        }

        return id.equals(((Id) obj).id);
    }

    @Override
    public int hashCode() {
        return id.hashCode();
    }

    public static Id of(String id) {
        return new Id(id);
    }

}
