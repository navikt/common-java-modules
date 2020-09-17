package no.nav.common.types.identer;

import com.fasterxml.jackson.annotation.JsonCreator;

/**
 * Representerer IDen til en NAV ansatt. Som oftest veileder/saksbehandler.
 * Eksempel: Z123456
 */
public class NavIdent extends InternBrukerId {

    @JsonCreator
    public NavIdent(String id) {
        super(id);
    }

    public static NavIdent of(String navIdent) {
        return new NavIdent(navIdent);
    }

}
