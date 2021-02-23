package no.nav.common.types.identer;

import com.fasterxml.jackson.annotation.JsonCreator;

/**
 * Representerer enten et fødselsnummer eller D-nummer for en ekstern bruker.
 */
public class NorskIdent extends EksternBrukerId {

    @JsonCreator
    public NorskIdent(String ident) {
        super(ident);
    }

    public static NorskIdent of(String ident) {
        return new NorskIdent(ident);
    }

}
