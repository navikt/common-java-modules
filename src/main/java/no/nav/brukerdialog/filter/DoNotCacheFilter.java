package no.nav.brukerdialog.filter;

import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerResponseContext;
import javax.ws.rs.container.ContainerResponseFilter;
import javax.ws.rs.core.Context;
import javax.ws.rs.ext.Provider;
import java.io.IOException;

@Provider
@DoNotCache
/**
 * Dette er et filter som sørger for at browseren ikke cacher forespørselen.
 *
 * Det vil være applikasjonsspesifikt om dette filtere brukes, og på hvilke ressurser.
 *
 */
public class DoNotCacheFilter implements ContainerResponseFilter {

    @Context
    HttpServletResponse resp;

    @Override
    public void filter(ContainerRequestContext containerRequestContext, ContainerResponseContext containerResponseContext) throws IOException {
        resp.setHeader("Cache-control", "no-cache, no-store, must-revalidate");
        resp.setHeader("Pragma", "no-cache");
        resp.setHeader("Expires", "0");
    }
}
