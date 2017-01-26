package no.nav.fo.security.jwt.filter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.core.Cookie;
import java.util.Optional;

public abstract class TokenLocator {

    private static final Logger log = LoggerFactory.getLogger(TokenLocator.class);

    static Optional<String> tokenValueFromAuthorizationHeader(ContainerRequestContext requestContext) {
        String headerValue = requestContext.getHeaderString("Authorization");
        if (headerValue == null || headerValue.isEmpty()) {
            return Optional.empty();
        } else {
            if (headerValue.startsWith("Bearer ")) {
                return Optional.ofNullable(headerValue.substring("Bearer ".length()));
            } else {
                log.warn("http header 'Authorization' without 'Bearer'.");
                return Optional.ofNullable(headerValue);
            }
        }
    }

    static Optional<String> tokenValueFromCookie(ContainerRequestContext requestContext) {
        Cookie authenticationCookie = requestContext.getCookies().get("Authentication");
        if (authenticationCookie == null || authenticationCookie.getValue() == null) {
            return Optional.empty();
        } else {
            log.warn("Authentication-cookie: " + authenticationCookie.getValue());
            return Optional.of(authenticationCookie.getValue());
        }
    }
}
