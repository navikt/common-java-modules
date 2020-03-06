package no.nav.common.aktorregisterklient;

import java.util.Optional;

public class IdentOppslag {

    /**
     * Identen(fnr/aktør id) som det ble gjort oppslag på i aktørregisteret
     */
    private String identTilRegister;

    /**
     * Identen(fnr/aktør id) som det ble returnert fra aktørregisteret
     */
    private String identFraRegister;

    public IdentOppslag(String identTilRegister, String identFraRegister) {
        this.identTilRegister = identTilRegister;
        this.identFraRegister = identFraRegister;
    }

    public String getIdentTilRegister() {
        return identTilRegister;
    }

    public Optional<String> getIdentFraRegister() {
        return Optional.ofNullable(identFraRegister);
    }

}