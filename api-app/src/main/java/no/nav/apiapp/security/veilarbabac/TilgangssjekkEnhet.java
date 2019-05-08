package no.nav.apiapp.security.veilarbabac;

import org.slf4j.Logger;

import java.util.Optional;
import java.util.function.Supplier;

import static no.nav.apiapp.security.veilarbabac.MetrikkLogger.Tilgangstype.Enhet;
import static org.slf4j.LoggerFactory.getLogger;

class TilgangssjekkEnhet {

    private static final Logger logger = getLogger(TilgangssjekkEnhet.class);

    private Supplier<Boolean> veilarbAbacSjekker;
    private Supplier<Boolean> abacSjekker;
    private MetrikkLogger metrikk  = new MetrikkLogger(logger,"read",()->"");

    private Boolean foretrekkVeilarbAbac = false;
    private Boolean sammenliknTilgang = false;

    TilgangssjekkEnhet() {
    }

    TilgangssjekkEnhet metrikkLogger(Logger logger, String action, Supplier<String> idSupplier) {
        this.metrikk = new MetrikkLogger(logger,action,idSupplier);
        return this;
    }

    TilgangssjekkEnhet veilarbAbacSjekker(Supplier<Boolean> veilarbAbacSjekker) {
        this.veilarbAbacSjekker = veilarbAbacSjekker;
        return this;
    }

    TilgangssjekkEnhet abacSjekker(Supplier<Boolean> abacSjekker) {
        this.abacSjekker = abacSjekker;
        return this;
    }


    TilgangssjekkEnhet foretrekkVeilarbAbac(boolean foretrekkVeilarbAbac) {
        this.foretrekkVeilarbAbac = foretrekkVeilarbAbac;
        return this;
    }

    TilgangssjekkEnhet sammenliknTilgang(boolean sammenlikntilgang) {
        this.sammenliknTilgang = sammenlikntilgang;
        return this;
    }


    boolean sjekkTilgangTilEnhet() {

        boolean harTilgang;

        if (sammenliknTilgang) {
            harTilgang = sjekkOgSammenliknTilgang();
        } else {
            harTilgang = sjekkTilgang();
        }

        metrikk.loggMetrikk(Enhet,foretrekkVeilarbAbac);

        return harTilgang;
    }

    private Boolean sjekkTilgang() {
        Optional<Boolean> veilarbAbacResultat = Optional.empty();

        if(foretrekkVeilarbAbac) {
            veilarbAbacResultat = tryggSjekkTilgangVeilarbAbac();
        }

        return veilarbAbacResultat.orElse(tryggSjekkTilgangAbac().orElse(false));
      }

    private boolean sjekkOgSammenliknTilgang() {

        Optional<Boolean> veilarbAbacResultat = tryggSjekkTilgangVeilarbAbac();
        Optional<Boolean> abacResultat = tryggSjekkTilgangAbac();

        if (veilarbAbacResultat.isPresent() && abacResultat.isPresent() && !veilarbAbacResultat.equals(abacResultat)) {
            metrikk.erAvvik();
        }

        return foretrekkVeilarbAbac
                ? veilarbAbacResultat.orElse(abacResultat.orElse(false))
                : abacResultat.orElse(veilarbAbacResultat.orElse(false));
     }

    private Optional<Boolean> tryggSjekkTilgangAbac() {
        try {
            return Optional.of(abacSjekker.get());
        } catch(Throwable e) {
            logger.error("Kall mot Abac (enhetId) feiler", e);
        }
        return Optional.empty();
    }

    private Optional<Boolean> tryggSjekkTilgangVeilarbAbac() {
        try {
            return Optional.of(veilarbAbacSjekker.get());
        } catch(Throwable e) {
            logger.error("Kall mot veilarbAbac (enhetId) feiler", e);
        }
        return Optional.empty();
    }

}
