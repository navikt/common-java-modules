package no.nav.fo.security.jwt.filter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.container.ContainerRequestContext;
import java.util.Optional;

public class TokenLocator {

    private static final Logger log = LoggerFactory.getLogger(TokenLocator.class);

    private TokenLocator() {

    }

    static Optional<String> tokenValue(ContainerRequestContext requestContext) {
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
}
