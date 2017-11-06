package no.nav.apiapp.feil;

import static no.nav.apiapp.feil.Feil.Type.INGEN_TILGANG;

public class IngenTilgang extends Feil {

    public IngenTilgang() {
        super(INGEN_TILGANG);
    }

    @SuppressWarnings("unused")
    public IngenTilgang(String feilmelding) {
        super(INGEN_TILGANG, feilmelding);
    }

    @SuppressWarnings("unused")
    public IngenTilgang(Throwable e) {
        super(INGEN_TILGANG, e);
    }

}
