package no.nav.apiapp.security.veilarbabac;

import no.nav.apiapp.feil.IngenTilgang;
import org.slf4j.Logger;

import java.util.function.Supplier;

import static no.nav.apiapp.security.veilarbabac.MetrikkLogger.Tilgangstype.PersonAktoerId;
import static no.nav.apiapp.security.veilarbabac.MetrikkLogger.Tilgangstype.PersonFoedselsnummer;
import static org.slf4j.LoggerFactory.getLogger;

class TilgangssjekkBruker {

    private static final Logger logger = getLogger(TilgangssjekkBruker.class);

    private MetrikkLogger metrikk = new MetrikkLogger(logger,"read",()->"");
    private Supplier<Boolean> veilarbabacFnrSjekker;
    private Supplier<Boolean> veilarbabacAktoerIdSjekker;
    private Runnable abacFnrSjekker = ()->{};

    private Boolean brukAktoerId = false;
    private Boolean sammenliknTilgang = false;
    private Boolean foretrekkVeilarbAbac = false;

    TilgangssjekkBruker() {
    }

    TilgangssjekkBruker metrikkLogger(Logger logger, String action, Supplier<String> idSupplier) {
        this.metrikk = new MetrikkLogger(logger,action,idSupplier);
        return this;
    }

    TilgangssjekkBruker veilarbAbacFnrSjekker(Supplier<Boolean> veilarbabacFnrSjekker) {
        this.veilarbabacFnrSjekker = veilarbabacFnrSjekker;
        return this;
    }

    TilgangssjekkBruker veilarbAbacAktoerIdSjekker(Supplier<Boolean> veilarbabacAktoerIdSjekker) {
        this.veilarbabacAktoerIdSjekker = veilarbabacAktoerIdSjekker;
        return this;
    }

    TilgangssjekkBruker abacFnrSjekker(Runnable abacFnrSjekker) {
        this.abacFnrSjekker = abacFnrSjekker;
        return this;
    }

    TilgangssjekkBruker foretrekkVeilarbAbac(boolean foretrekkVeilarbAbac) {
        this.foretrekkVeilarbAbac = foretrekkVeilarbAbac;
        return this;
    }

    TilgangssjekkBruker sammenliknTilgang(boolean sammenlikntilgang) {
        this.sammenliknTilgang = sammenlikntilgang;
        return this;
    }

    TilgangssjekkBruker brukAktoerId(boolean brukAktoerId) {
        this.brukAktoerId = brukAktoerId;
        return this;
    }

    void sjekkTilgangTilBruker() {


        boolean harTilgang;

        if (brukAktoerId && sammenliknTilgang) {
            harTilgang = sjekkOgSammenliknTilgangForAktoerIdOgFnr();
        } else if (sammenliknTilgang) {
            harTilgang = sjekkOgSammenliknTilgangForFnr();
        } else if (brukAktoerId) {
            harTilgang = sjekkTilgangTilAktoerId();
        } else {
            harTilgang = sjekkAbacTilgangTilFnr();
        }

        metrikk.loggMetrikk(brukAktoerId ? PersonAktoerId : PersonFoedselsnummer);

        if (!harTilgang) {
            throw new IngenTilgang();
        }
    }

    private boolean sjekkTilgangTilAktoerId() {
        return veilarbabacAktoerIdSjekker.get();
    }

    private boolean sjekkOgSammenliknTilgangForFnr() {
        Boolean veilarbAbacResultat=null;

        try {
            veilarbAbacResultat = veilarbabacFnrSjekker.get();
        } catch (Throwable e) {
            // Ignorer feilen. Vi kjører videre med Abac direkte
            logger.error("Kall mot veilarbAbac feiler", e);
        }

        boolean abacResultat = sjekkAbacTilgangTilFnr();

        if (veilarbAbacResultat!=null && abacResultat != veilarbAbacResultat) {
            metrikk.erAvvik();
        }

        if(veilarbAbacResultat!=null && foretrekkVeilarbAbac) {
            return veilarbAbacResultat;
        } else {
            return abacResultat;
        }
    }

    private boolean sjekkOgSammenliknTilgangForAktoerIdOgFnr() {
        boolean fnrResultat = veilarbabacFnrSjekker.get();
        boolean aktoerIdResultat = veilarbabacAktoerIdSjekker.get();

        if (fnrResultat != aktoerIdResultat) {
            metrikk.erAvvik();;
        }

        // Stoler mest på fnr-resultatet
        return fnrResultat;
    }

    private boolean sjekkAbacTilgangTilFnr() {
        try {
            abacFnrSjekker.run();
            return true;
        } catch (IngenTilgang e) {
            return false;
        }
    }

}
