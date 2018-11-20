package no.nav.fo.apiapp.rest;

import io.micrometer.core.instrument.Counter;
import no.nav.apiapp.feil.Feil;
import no.nav.apiapp.feil.VersjonsKonflikt;
import no.nav.metrics.MetricsFactory;

import javax.ws.rs.GET;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.core.Response;
import java.util.Map;

@Path("/eksempel")
public class MetricEksempel {

    private static final Counter EKSEMPEL = MetricsFactory.getMeterRegistry().counter("eksempel");

    @GET
    public String get() {
        EKSEMPEL.increment();
        return "eksempel";
    }


}
