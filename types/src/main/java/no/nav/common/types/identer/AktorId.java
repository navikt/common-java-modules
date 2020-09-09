package no.nav.common.types.identer;

import com.fasterxml.jackson.annotation.JsonValue;

/**
 * Representerer aktør IDen til en bruker.
 * Eksempel: 1112223334445
 */
public class AktorId {

    private final String aktorId;

    private AktorId(String aktorId) {
        if (aktorId == null) {
            throw new IllegalArgumentException("AktorId cannot be null");
        }

        this.aktorId = aktorId;
    }

    public static AktorId of(String aktorIdStr) {
        return new AktorId(aktorIdStr);
    }

    public String get() {
        return aktorId;
    }

    @JsonValue
    @Override
    public String toString() {
        return aktorId;
    }

}
