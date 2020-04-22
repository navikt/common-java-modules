package no.nav.common.types.feil;

public class IngenTilgang extends Feil {

    public IngenTilgang() {
        super(FeilType.INGEN_TILGANG);
    }

    @SuppressWarnings("unused")
    public IngenTilgang(String feilmelding) {
        super(FeilType.INGEN_TILGANG, feilmelding);
    }

    @SuppressWarnings("unused")
    public IngenTilgang(Throwable e) {
        super(FeilType.INGEN_TILGANG, e);
    }

}
