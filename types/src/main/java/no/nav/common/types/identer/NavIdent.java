package no.nav.common.types.identer;

/**
 * Representerer IDen til en NAV ansatt. Som oftest veileder/saksbehandler.
 * Eksempel: Z123456
 */
public class NavIdent extends InternBrukerId {

    private NavIdent(String id) {
        super(id);
    }

    public static NavIdent of(String navIdent) {
        return new NavIdent(navIdent);
    }

}
