package no.nav.apiapp.feil;

import static no.nav.apiapp.feil.FeilType.VERSJONSKONFLIKT;

@Deprecated
public class VersjonsKonflikt extends Feil {

    public VersjonsKonflikt() {
        super(VERSJONSKONFLIKT);
    }

}
