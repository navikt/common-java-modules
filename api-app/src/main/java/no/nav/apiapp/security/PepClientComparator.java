package no.nav.apiapp.security;

import no.nav.apiapp.feil.IngenTilgang;
import org.slf4j.Logger;

import java.util.function.Supplier;

import static org.slf4j.LoggerFactory.getLogger;

public class PepClientComparator {

    private static PepClientComparatorImpl comparator = new PepClientComparatorImpl();

    public static void get(Deferred orginal, Deferred sammenligneMed) {
        comparator.get(orginal, sammenligneMed);
    }

    public static boolean get(Supplier<Boolean> orginal, Supplier<Boolean> sammenligneMed) {
        return comparator.get(orginal, sammenligneMed);
    }

}

/**
 * Temporary comparator for regression testing
 */
class PepClientComparatorImpl {

    private Logger log = getLogger(PepClientComparatorImpl.class);

    public PepClientComparatorImpl() {
    }

    PepClientComparatorImpl(Logger log) {
        this.log = log;
    }

    boolean get(Supplier<Boolean> orginal, Supplier<Boolean> sammenligneMed) {
        boolean orginalResultat = orginal.get();

        Boolean sammenligneMedResultat = null;
        boolean sammenligneMedFeilet = false;
        try {
            sammenligneMedResultat = sammenligneMed.get();
        } catch (Throwable t) {
            sammenligneMedFeilet = true;
            log.warn("Feil i kall mot pep for sammenligning", t);
        }

        boolean isEqual = sammenligneMedResultat != null && orginalResultat == sammenligneMedResultat;

        if (!isEqual && !sammenligneMedFeilet) {
            String expected = orginalResultat ? "tilgang" : "ingen tilgang";
            String got = sammenligneMedResultat != null ? (sammenligneMedResultat ? "tilgang" : "ingen tilgang") : "<ingen verdi>";
            log.warn("Avvik i resultat fra pep sammenligning. Forventet " + expected + " fikk " + got);
        }

        return orginalResultat;
    }

    void get(Deferred orginal, Deferred sammenligneMed) {
        IngenTilgang ingenTilgangOrginal = null;

        try {
            orginal.get();
        } catch (IngenTilgang e) {
            ingenTilgangOrginal = e;
        }

        IngenTilgang ingenTilgangSecondary = null;
        boolean sammenligneMedFeilet = false;
        try {
            sammenligneMed.get();
        } catch (IngenTilgang e) {
            ingenTilgangSecondary = e;
        } catch (Throwable t) {
            sammenligneMedFeilet = true;
            log.warn("Feil i kall mot pep for sammenligning", t);
        }

        boolean isEqual =
                (ingenTilgangOrginal != null && ingenTilgangSecondary != null) ||
                        (ingenTilgangOrginal == null && ingenTilgangSecondary == null);

        if (!isEqual && !sammenligneMedFeilet) {
            String expected = ingenTilgangOrginal != null ? "ingen tilgang" : "tilgang";
            String got = ingenTilgangSecondary != null ? "ingen tilgang" : "tilgang";
            log.warn("Avvik i resultat fra pep sammenligning. Forventet " + expected + " fikk " + got);
        }


        if (ingenTilgangOrginal != null) {
            throw ingenTilgangOrginal;
        }
    }
}

interface Deferred {

    void get();
}
