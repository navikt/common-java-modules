package no.nav.fo.security.jwt.filter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Priority;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerResponseContext;
import javax.ws.rs.container.ContainerResponseFilter;
import javax.ws.rs.core.Context;
import javax.ws.rs.ext.Provider;
import java.io.IOException;

@Provider
@SessionTerminator
@Priority(Integer.MAX_VALUE) //terminering av session er siste filter.
/**
 * Dette er et filter som avslutter session etter hver forespøsel.
 *
 * Dette sikrer at forespørsler er tilstandsløse.
 */
public class SessionTerminatorFilter implements ContainerResponseFilter {

    private static final Logger log = LoggerFactory.getLogger(SessionTerminatorFilter.class);

    @Context
    HttpServletRequest req;

    @Override
    public void filter(ContainerRequestContext containerRequestContext, ContainerResponseContext containerResponseContext) throws IOException {
        HttpSession session = req.getSession(false);
        if (session != null) {
            String sessionDescription = session.toString();
            session.invalidate();
            log.info("Invalidated session " + sessionDescription);
        }
    }
}
