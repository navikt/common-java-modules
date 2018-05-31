package no.nav.fo.apiapp.rest;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;
import java.net.URI;

import static no.nav.fo.apiapp.ApplicationConfig.APPLICATION_NAME;

@Path("/redirect")
public class RedirectEksempel {

    @GET
    public Response redirect(@Context UriInfo uriBuilder) {
        URI build = uriBuilder.getRequestUriBuilder()
                .replacePath(APPLICATION_NAME)
                .path("api")
                .path("ping")
                .build();
        return Response.temporaryRedirect(build).build();
    }

}
