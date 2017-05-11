package no.nav.fo.apiapp.rest;

import no.nav.apiapp.feil.VersjonsKonflikt;

import javax.ws.rs.GET;
import javax.ws.rs.Path;

@Path("/eksempel")
public class RestEksempel {

    @GET
    public String get() {
        return "eksempel";
    }

    @GET
    @Path("/konflikt")
    public String konflikt() {
        throw new VersjonsKonflikt();
    }

    @GET
    @Path("/ukjent-feil")
    public String ukjentFeil() {
        throw new RuntimeException();
    }

}
