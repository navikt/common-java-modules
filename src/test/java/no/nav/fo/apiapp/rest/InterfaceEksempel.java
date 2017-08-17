package no.nav.fo.apiapp.rest;

import javax.ws.rs.GET;
import javax.ws.rs.Path;

@Path("/interface")
public interface InterfaceEksempel {

    @GET
    @Path("/impl")
    String implementertGet();

    @GET
    @Path("/default")
    default String defaultGet() {
        return "fra interface!";
    }

}
