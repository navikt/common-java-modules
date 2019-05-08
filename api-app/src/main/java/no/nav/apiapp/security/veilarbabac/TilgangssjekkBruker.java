package no.nav.apiapp.security.veilarbabac;

import no.nav.apiapp.feil.IngenTilgang;
import org.slf4j.Logger;

import java.util.Optional;
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
            harTilgang = tryggVeilarbabacAktorIdSjekker().orElse(false);
        } else {
            harTilgang = tryggAbacFnrSjekker().orElse(false);
        }

        metrikk.loggMetrikk(brukAktoerId ? PersonAktoerId : PersonFoedselsnummer,foretrekkVeilarbAbac);

        if (!harTilgang) {
            throw new IngenTilgang();
        }
    }

    private boolean sjekkOgSammenliknTilgangForFnr() {
        Optional<Boolean> veilarbAbacResultat = tryggVeilarbabacFnrSjekker();
        Optional<Boolean> abacResultat = tryggAbacFnrSjekker();

        if (veilarbAbacResultat.isPresent() && abacResultat.isPresent() && !abacResultat.equals(veilarbAbacResultat)) {
            metrikk.erAvvik();
        }

        return foretrekkVeilarbAbac
                ? veilarbAbacResultat.orElse(abacResultat.orElse(false))
                : abacResultat.orElse(veilarbAbacResultat.orElse(false));

      }

    private boolean sjekkOgSammenliknTilgangForAktoerIdOgFnr() {
        Optional<Boolean> aktoerIdResultat = tryggVeilarbabacAktorIdSjekker();
        Optional<Boolean> fnrResultat = tryggVeilarbabacFnrSjekker();

        if (fnrResultat.isPresent() && aktoerIdResultat.isPresent() && !fnrResultat.equals(aktoerIdResultat)) {
            metrikk.erAvvik();;
        }

        // Stoler mest på fnr-resultatet hvis begge finnes, men prøver å gi et resultat
        return fnrResultat.orElse(aktoerIdResultat.orElse(false));
    }

    private Optional<Boolean> tryggVeilarbabacFnrSjekker() {
        Optional<Boolean> fnrResultat=Optional.empty();

        try {
            fnrResultat = Optional.of(veilarbabacFnrSjekker.get());
        } catch(Throwable e) {
            logger.error("Kall mot veilarbAbac (fnr) feiler", e);
        }
        return fnrResultat;
    }

    private Optional<Boolean> tryggVeilarbabacAktorIdSjekker() {
        Optional<Boolean> aktoerIdResultat=Optional.empty();

        try {
            aktoerIdResultat = Optional.of(veilarbabacAktoerIdSjekker.get());
        } catch(Throwable e) {
            // Ignorer feilen. Vi kjører videre med fnr
            logger.error("Kall mot veilarbAbac (aktørId) feiler", e);
        }
        return aktoerIdResultat;
    }

    private Optional<Boolean> tryggAbacFnrSjekker() {
        try {
            abacFnrSjekker.run();
            return Optional.of(true);
        } catch (IngenTilgang e) {
            return Optional.of(false);
        }
        catch(Throwable e) {
            logger.error("Kall mot abac feiler", e);
        }
        return Optional.empty();
    }
}
