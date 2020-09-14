package no.nav.common.types.identer;

/**
 * Representerer IDen til en NAV enhet.
 * Eksempel: 0123
 */
public class EnhetId extends Id {

    private EnhetId(String id) {
        super(id);
    }

    public static EnhetId of(String enhetId) {
        return new EnhetId(enhetId);
    }

}
