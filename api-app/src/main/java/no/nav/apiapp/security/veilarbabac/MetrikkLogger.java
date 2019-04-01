package no.nav.apiapp.security.veilarbabac;

import io.micrometer.core.instrument.MeterRegistry;
import no.nav.common.auth.SubjectHandler;
import no.nav.metrics.MetricsFactory;
import org.slf4j.Logger;

import java.util.function.Supplier;

class MetrikkLogger {

    private final MeterRegistry meterRegistry = MetricsFactory.getMeterRegistry();

    private boolean erAvvik = false;
    private String action = "read";
    private Supplier<String> idSupplier = ()->"";
    private Logger logger;

    MetrikkLogger(Logger logger, String action, Supplier<String> idSupplier) {
        this.action = action;
        this.idSupplier = idSupplier;
        this.logger = logger;
    }

    public MetrikkLogger logger(Logger logger) {
        this.logger = logger;
        return this;
    }

    void erAvvik() {
        this.erAvvik = true;
    }

    void loggMetrikk(Tilgangstype tilgangstype) {
        meterRegistry.counter("veilarabac-abac-pep",
                "tilgangstype",
                tilgangstype.name(),
                "identType",
                SubjectHandler.getIdentType().map(Enum::name).orElse("unknown"),
                "action",
                action,
                "avvik",
                Boolean.toString(erAvvik)
        ).increment();

        if(erAvvik) {
            logger.warn("Fikk avvik i tilgang for %s", idSupplier.get());
        }
    }

    enum Tilgangstype {
        PersonAktoerId,
        PersonFoedselsnummer,
        Enhet
    }
}
