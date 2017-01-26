package no.nav.fo.security.jwt.filter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerRequestFilter;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import java.io.IOException;
import java.util.Optional;

abstract public class AbstractJWTJAASFilter implements ContainerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(AbstractJWTJAASFilter.class);

    @Context
    HttpServletRequest req;

    @Override
    public void filter(ContainerRequestContext requestContext) throws IOException {
        if (req.getRemoteUser() != null) {
            log.info("User already logged in as {}. Skipping login.", req.getRemoteUser());
            return;
        }

        Optional<String> jwt = getJwt(requestContext);
        if (!jwt.isPresent()) {
            requestContext.abortWith(Response.status(Status.UNAUTHORIZED).build());
            return;
        }

        try {
            req.login(jwt.get(), jwt.get());

            log.debug("SecurityContext Scheme after login: {}.", requestContext.getSecurityContext().getAuthenticationScheme());
            log.debug("ScurityContext Principal after login: {}.", requestContext.getSecurityContext().getUserPrincipal());

        } catch (Exception e) {
            log.info("Feil ved innlogging av bruker.", e);
            StatusMessage sm = new StatusMessage("Login failed", Status.UNAUTHORIZED);
            requestContext.abortWith(Response.status(Status.UNAUTHORIZED)
                    .status(Status.UNAUTHORIZED)
                    .entity(sm)
                    .build());
        }

    }

    abstract Optional<String> getJwt(ContainerRequestContext requestContext);

}
