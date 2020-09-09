package no.nav.common.types.identer;

import com.fasterxml.jackson.annotation.JsonValue;

/**
 * Representerer IDen til en vilkårlig person. Kan være saksbehandler, ekstern bruker eller noe annet.
 * Eksempel: Z123456, 12345678901, 7d713b4a-f2b1-11ea-adc1-0242ac120002
 */
public class PersonId {

    private final String personId;

    private PersonId(String personId) {
        if (personId == null) {
            throw new IllegalArgumentException("PersonId cannot be null");
        }

        this.personId = personId;
    }

    public static PersonId of(String personIdStr) {
        return new PersonId(personIdStr);
    }

    public String get() {
        return personId;
    }

    @JsonValue
    @Override
    public String toString() {
        return personId;
    }

}
