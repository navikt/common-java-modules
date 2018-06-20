package no.nav.fo.apiapp.rest;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;
import java.net.URI;

@Path("/redirect")
public class RedirectEksempel {

    @GET
    public Response redirect(@Context UriInfo uriBuilder, @Context HttpServletRequest httpServletRequest) {
        URI build = uriBuilder.getRequestUriBuilder()
                .replacePath(httpServletRequest.getContextPath())
                .path("api")
                .path("ping")
                .build();
        return Response.temporaryRedirect(build).build();
    }

}
