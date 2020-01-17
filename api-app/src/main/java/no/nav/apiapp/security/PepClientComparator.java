package no.nav.apiapp.security;

import no.nav.apiapp.feil.IngenTilgang;
import no.nav.sbl.util.fn.DeferredVoid;
import org.slf4j.Logger;

import java.util.function.Supplier;

import static org.slf4j.LoggerFactory.getLogger;

/**
 * Temporary comparator for regression testing
 */
public class PepClientComparator {

    private static PepClientComparatorImpl comparator = new PepClientComparatorImpl();

    public static void get(DeferredVoid orginal, DeferredVoid sammenligneMed) {
        comparator.get(orginal, sammenligneMed);
    }

    public static boolean get(Supplier<Boolean> orginal, Supplier<Boolean> sammenligneMed) {
        return comparator.get(orginal, sammenligneMed);
    }

}

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

        if (!isEqual && !sammenligneMedFeilet && sammenligneMedResultat != null) {
            String expected = orginalResultat ? "permit" : "deny";
            String got = sammenligneMedResultat ? "permit" : "deny";
            log.warn("Avvik i resultat fra pep sammenligning. Forventet " + expected + " fikk " + got + ".");
        }

        return orginalResultat;
    }

    void get(DeferredVoid orginal, DeferredVoid sammenligneMed) {
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
            String expected = ingenTilgangOrginal != null ? "deny" : "permit";
            String got = ingenTilgangSecondary != null ? "deny" : "permit";
            log.warn("Avvik i resultat fra pep sammenligning. Forventet " + expected + " fikk " + got + ".");
        }

        if (ingenTilgangOrginal != null) {
            throw ingenTilgangOrginal;
        }
    }
}
