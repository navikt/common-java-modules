package no.nav.sbl.dialogarena.types.feil;

import static no.nav.sbl.dialogarena.types.feil.FeilType.INGEN_TILGANG;

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
