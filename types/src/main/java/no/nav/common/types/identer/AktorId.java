package no.nav.common.types.identer;

/**
 * Representerer aktør IDen til en bruker.
 * Eksempel: 1112223334445
 */
public class AktorId extends EksternBrukerId {

    private AktorId(String id) {
        super(id);
    }

    public static AktorId of(String aktorIdStr) {
        return new AktorId(aktorIdStr);
    }

}
