package no.nav.fo.security.jwt.filter;


import javax.annotation.Priority;
import javax.ws.rs.Priorities;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.ext.Provider;
import java.util.Optional;

@JWTInAuthorizationHeaderJAAS
@Provider
@Priority(Priorities.AUTHENTICATION)
/*
 * Performs JAAS container login based on JWT in Authorization header
 * Assumes JWT has been validated
 */
public class JWTInAuthorizationHeaderJAASFilter extends AbstractJWTJAASFilter {


    @Override
    Optional<String> getJwt(ContainerRequestContext requestContext) {
        return TokenLocator.tokenValueFromAuthorizationHeader(requestContext);
    }

}
