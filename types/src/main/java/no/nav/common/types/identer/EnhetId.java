package no.nav.common.types.identer;

import com.fasterxml.jackson.annotation.JsonValue;

/**
 * Representerer IDen til en NAV enhet.
 * Eksempel: 0123
 */
public class EnhetId {

    private final String enhetId;

    private EnhetId(String enhetId) {
        if (enhetId == null) {
            throw new IllegalArgumentException("EnhetId cannot be null");
        }

        this.enhetId = enhetId;
    }

    public static EnhetId of(String enhetIdStr) {
        return new EnhetId(enhetIdStr);
    }

    public String get() {
        return enhetId;
    }

    @JsonValue
    @Override
    public String toString() {
        return enhetId;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) {
            return true;
        }

        if (!(obj instanceof EnhetId)) {
            return false;
        }

        return enhetId.equals(((EnhetId) obj).enhetId);
    }

    @Override
    public int hashCode() {
        return enhetId.hashCode();
    }

}
