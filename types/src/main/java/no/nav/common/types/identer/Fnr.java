package no.nav.common.types.identer;

import com.fasterxml.jackson.annotation.JsonValue;

/**
 * Representerer fødselsnummeret til en bruker.
 * Eksempel: 12345678901
 */
public class Fnr {

    private final String fnr;

    private Fnr(String fnr) {
        if (fnr == null) {
            throw new IllegalArgumentException("Fnr cannot be null");
        }

        this.fnr = fnr;
    }

    public static Fnr of(String fnrStr) {
        return new Fnr(fnrStr);
    }

    public String get() {
        return fnr;
    }

    @JsonValue
    @Override
    public String toString() {
        return fnr;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) {
            return true;
        }

        if (!(obj instanceof Fnr)) {
            return false;
        }

        return fnr.equals(((Fnr) obj).fnr);
    }

    @Override
    public int hashCode() {
        return fnr.hashCode();
    }

}
