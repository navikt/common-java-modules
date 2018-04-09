package no.nav.apiapp.feil;

import static no.nav.apiapp.feil.Feil.Type.UGYLDIG_REQUEST;

@SuppressWarnings("unused")
public class UgyldigRequest extends Feil {

    public UgyldigRequest() {
        super(UGYLDIG_REQUEST);
    }

}
