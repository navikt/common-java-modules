package no.nav.common.auth.oidc.filter;

import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.proc.BadJOSEException;
import com.nimbusds.jwt.JWT;
import com.nimbusds.jwt.JWTParser;
import com.nimbusds.openid.connect.sdk.validators.BadJWTExceptions;
import no.nav.common.auth.context.AuthContext;
import no.nav.common.auth.context.AuthContextHolder;
import no.nav.common.auth.oidc.TokenRefreshClient;
import no.nav.common.auth.utils.CookieUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.*;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.text.ParseException;
import java.util.Date;
import java.util.List;
import java.util.Optional;

import static no.nav.common.auth.utils.TokenUtils.*;


public class OidcAuthenticationFilter implements Filter {
    private static final Logger logger = LoggerFactory.getLogger(OidcAuthenticationFilter.class);

    // Check if the token is about to expire within the next 5 minutes
    private static final long CHECK_EXPIRES_WITHIN = 1000 * 60 * 5;

    private final List<OidcAuthenticator> oidcAuthenticators;

    private final TokenRefreshClient tokenRefreshClient;

    public OidcAuthenticationFilter(List<OidcAuthenticator> oidcAuthenticators) {
        this.oidcAuthenticators = oidcAuthenticators;
        tokenRefreshClient = new TokenRefreshClient();
    }

    @Override
    public void init(FilterConfig filterConfig) {}

    @Override
    public void doFilter(ServletRequest servletRequest, ServletResponse servletResponse, FilterChain chain) {
        HttpServletRequest request = (HttpServletRequest) servletRequest;
        HttpServletResponse response = (HttpServletResponse) servletResponse;

        for (OidcAuthenticator authenticator : oidcAuthenticators) {

            Optional<String> token = authenticator.findIdToken(request);

            if (token.isPresent()) {
                try {
                    JWT jwtToken = JWTParser.parse(token.get());

                    // Skip this authenticator if the audience is not matching
                    if (!hasMatchingAudience(jwtToken, authenticator.config.clientIds)) {
                        continue;
                    }

                    Optional<String> refreshedIdToken = refreshIdTokenIfNecessary(jwtToken, authenticator, request);

                    if (refreshedIdToken.isPresent()) {
                        jwtToken = JWTParser.parse(refreshedIdToken.get());

                        String idTokenCookieName = authenticator.config.idTokenCookieName;
                        addNewIdTokenCookie(idTokenCookieName, jwtToken, request, response);
                    }

                    authenticator.tokenValidator.validate(jwtToken);

                    AuthContext authContext = new AuthContext(authenticator.config.userRole, jwtToken);

                    AuthContextHolder.withContext(authContext, () -> chain.doFilter(servletRequest, servletResponse));
                    return;
                } catch (ParseException | JOSEException | BadJOSEException exception) {
                    if (exception == BadJWTExceptions.EXPIRED_EXCEPTION) {
                        logger.info("Token validation failed", exception);
                    } else {
                        logger.error("Token validation failed", exception);
                    }
                }
            }
        }

        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
    }

    private void addNewIdTokenCookie(
            String idTokenCookieName, JWT jwtToken, HttpServletRequest request, HttpServletResponse response
    ) throws ParseException {
        Date cookieExpiration = jwtToken.getJWTClaimsSet().getExpirationTime();
        Cookie newIdCookie = CookieUtils.createCookie(idTokenCookieName, jwtToken.getParsedString(), cookieExpiration, request);
        response.addCookie(newIdCookie);
    }

    private Optional<String> refreshIdTokenIfNecessary(JWT token, OidcAuthenticator authenticator, HttpServletRequest request) {
        boolean needsToBeRefreshed = hasMatchingIssuer(token, authenticator.tokenValidator.getIssuer())
                && expiresWithin(token, CHECK_EXPIRES_WITHIN);

        if (needsToBeRefreshed) {
            Optional<String> maybeRefreshToken = authenticator.findRefreshToken(request);

            if (maybeRefreshToken.isPresent() && authenticator.config.refreshUrl != null) {
                try {
                    return Optional.of(tokenRefreshClient.refreshIdToken(authenticator.config.refreshUrl, maybeRefreshToken.get()));
                } catch (Exception e) {
                    logger.error("Unable to refresh id token", e);
                    return Optional.empty();
                }
            }
        }

        return Optional.empty();
    }

    @Override
    public void destroy() {}

}
