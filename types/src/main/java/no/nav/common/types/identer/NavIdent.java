package no.nav.common.types.identer;

import com.fasterxml.jackson.annotation.JsonValue;

/**
 * Representerer IDen til en NAV ansatt. Som oftest veileder/saksbehandler.
 * Eksempel: Z123456
 */
public class NavIdent {

    private final String navIdent;

    private NavIdent(String navIdent) {
        if (navIdent == null) {
            throw new IllegalArgumentException("NavIdent cannot be null");
        }

        this.navIdent = navIdent;
    }

    public static NavIdent of(String navIdentStr) {
        return new NavIdent(navIdentStr);
    }

    public String get() {
        return navIdent;
    }

    @JsonValue
    @Override
    public String toString() {
        return navIdent;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) {
            return true;
        }

        if (!(obj instanceof NavIdent)) {
            return false;
        }

        return navIdent.equals(((NavIdent) obj).navIdent);
    }

    @Override
    public int hashCode() {
        return navIdent.hashCode();
    }

}
