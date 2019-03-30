package no.nav.apiapp.security.veilarbabac;

import java.util.function.Function;

import static no.nav.apiapp.util.StringUtils.nullOrEmpty;

public class Bruker {

    private String fnr;
    private String aktoerId;
    private Function<String, String> aktoerIdTilFnr;
    private Function<String, String> fnrTilAktoerId ;

    public static Builder ny() {
        return new Builder();
    }

    public static class Builder {

        Bruker bruker = new Bruker();

        public Builder medFoedeselsnummer(String fnr) {
            bruker.fnr = fnr;
            return this;
        }

        public Builder medAktoerId(String aktoerId) {
            bruker.aktoerId = aktoerId;
            return this;
        }

        public Builder medAktoerIdTilFoedselsnummerKonvertering(Function<String,String> aktoerIdTilFnr) {
            bruker.aktoerIdTilFnr=aktoerIdTilFnr;
            return this;
        }

        public Builder medFoedselnummerTilAktoerIdKonvertering(Function<String,String> fnrTilAktoerId) {
            bruker.fnrTilAktoerId=fnrTilAktoerId;
            return this;
        }

        public Bruker bygg() {
            if ((nullOrEmpty(bruker.fnr) && nullOrEmpty(bruker.aktoerId))) {
                throw new IllegalStateException("Bruker mangler både fødselsnummer og aktørId");
            }

            if (nullOrEmpty(bruker.fnr) && bruker.aktoerIdTilFnr==null) {
                throw new IllegalStateException("Bruker trenger fødselsnummer eller konverterer fra aktørId");
            }

            if (nullOrEmpty (bruker.aktoerId) && bruker.fnrTilAktoerId==null) {
                throw new IllegalStateException("Bruker trenger aktørId eller konverterer fra fnr");
            }

            return bruker;
        }

    }

    public String getFoedselsnummer() {
        if(fnr==null) {
            fnr = aktoerIdTilFnr.apply(aktoerId);
        }
        return fnr;
    }

    public String getAktoerId() {
        if(aktoerId==null) {
            aktoerId = fnrTilAktoerId.apply(fnr);
        }
        return aktoerId;
    }

    @Override
    public String toString() {
        return "Bruker{" +
                "fnr='" + fnr + '\'' +
                ", aktoerId='" + aktoerId + '\'' +
                '}';
    }
}
