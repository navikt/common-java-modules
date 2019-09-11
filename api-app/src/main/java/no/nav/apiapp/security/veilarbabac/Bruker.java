package no.nav.apiapp.security.veilarbabac;

import no.nav.util.sbl.StringUtils;

import java.util.Objects;
import java.util.function.Supplier;

public class Bruker {

    private String fnr, originaltFnr;
    private String aktoerId, originalAktoerId;
    private Supplier<String> aktoerIdSupplier;
    private Supplier<String> fnrSupplier;

    public static FnrBuilder fraFnr(String fnr) {
        if (StringUtils.nullOrEmpty(fnr)) {
            throw new IllegalArgumentException("fnr må ha verdi");
        }

        return new FnrBuilder(fnr);
    }

    public static AktoerIdBuilder fraAktoerId(String aktoerId) {
        if (StringUtils.nullOrEmpty(aktoerId)) {
            throw new IllegalArgumentException("aktoerId må ha verdi");
        }

        return new AktoerIdBuilder(aktoerId);
    }

    public String getFoedselsnummer() {
        if (fnr == null) {
            fnr = fnrSupplier.get();
        }
        return fnr;
    }

    public String getAktoerId() {
        if (aktoerId == null) {
            aktoerId = aktoerIdSupplier.get();
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

    // Regner bruker som lik hvis originalt fnr og/eller aktørId er like
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Bruker bruker = (Bruker) o;
        return Objects.equals(originaltFnr, bruker.originaltFnr) &&
                Objects.equals(originalAktoerId, bruker.originalAktoerId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(originaltFnr, originalAktoerId);
    }


    public static class FnrBuilder {

        Bruker bruker = new Bruker();

        private FnrBuilder(String fnr) {
            bruker.fnr = bruker.originaltFnr = fnr;
        }

        public Bruker medAktoerId(String aktoerId) {
            if (StringUtils.nullOrEmpty(aktoerId)) {
                throw new IllegalArgumentException("aktoerId må ha verdi");
            }

            bruker.aktoerId = bruker.originalAktoerId = aktoerId;

            return bruker;
        }

        public Bruker medAktoerIdSupplier(Supplier<String> aktoerIdSupplier) {
            bruker.aktoerIdSupplier = aktoerIdSupplier;
            return bruker;
        }

    }

    public static class AktoerIdBuilder {

        Bruker bruker = new Bruker();

        private AktoerIdBuilder(String aktoerId) {
            bruker.aktoerId = bruker.originalAktoerId = aktoerId;
        }

        public Bruker medFoedselsnummer(String fnr) {
            if (StringUtils.nullOrEmpty(fnr)) {
                throw new IllegalArgumentException("fnr må ha verdi");
            }

            bruker.fnr = bruker.originaltFnr = fnr;
            return bruker;
        }

        public Bruker medFoedselnummerSupplier(Supplier<String> fnrSupplier) {
            bruker.fnrSupplier = fnrSupplier;
            return bruker;
        }
    }
}
