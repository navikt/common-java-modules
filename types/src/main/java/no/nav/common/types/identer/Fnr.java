package no.nav.common.types.identer;

import com.fasterxml.jackson.annotation.JsonCreator;

/**
 * Representerer fødselsnummeret til en bruker.
 * Eksempel: 12345678901
 */
public class Fnr extends EksternBrukerId {

    @JsonCreator
    public Fnr(String id) {
        super(id);
    }

    public static Fnr of(String fnrStr) {
        return new Fnr(fnrStr);
    }

}
