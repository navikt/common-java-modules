package no.nav.common.types.identer;

import com.fasterxml.jackson.annotation.JsonCreator;

/**
 * Representerer IDen til en NAV enhet.
 * Eksempel: 0123
 */
public class EnhetId extends Id {

    @JsonCreator
    public EnhetId(String id) {
        super(id);
    }

    public static EnhetId of(String enhetId) {
        return new EnhetId(enhetId);
    }

}
