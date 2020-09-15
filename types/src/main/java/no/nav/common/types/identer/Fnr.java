package no.nav.common.types.identer;

/**
 * Representerer fødselsnummeret til en bruker.
 * Eksempel: 12345678901
 */
public class Fnr extends EksternBrukerId {

    private Fnr(String id) {
        super(id);
    }

    public static Fnr of(String fnrStr) {
        return new Fnr(fnrStr);
    }

}
