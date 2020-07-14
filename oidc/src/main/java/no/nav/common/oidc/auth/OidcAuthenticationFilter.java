package no.nav.common.oidc.auth;

import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.proc.BadJOSEException;
import com.nimbusds.jwt.JWT;
import com.nimbusds.jwt.JWTParser;
import com.nimbusds.openid.connect.sdk.validators.BadJWTExceptions;
import no.nav.common.auth.SsoToken;
import no.nav.common.auth.Subject;
import no.nav.common.auth.SubjectHandler;
import no.nav.common.oidc.TokenRefreshClient;
import no.nav.common.oidc.utils.CookieUtils;
import no.nav.common.oidc.utils.TokenUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.*;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.text.ParseException;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.regex.Pattern;

import static java.util.stream.Collectors.toList;
import static no.nav.common.oidc.utils.TokenUtils.*;

public class OidcAuthenticationFilter implements Filter {
    private static final Logger logger = LoggerFactory.getLogger(OidcAuthenticationFilter.class);

    // Check if the token is about to expire within the next 5 minutes
    private static final long CHECK_EXPIRES_WITHIN = 1000 * 60 * 5;

    private final List<OidcAuthenticator> oidcAuthenticators;

    private final List<String> publicPaths;

    private final TokenRefreshClient tokenRefreshClient;

    private List<Pattern> publicPatterns;

    public OidcAuthenticationFilter(List<OidcAuthenticator> oidcAuthenticators, List<String> publicPaths) {
        this.oidcAuthenticators = oidcAuthenticators;
        this.publicPaths = publicPaths;
        tokenRefreshClient = new TokenRefreshClient();
    }

    @Override
    public void init(FilterConfig filterConfig) {
        String contextPath = contextPath(filterConfig);
        this.publicPatterns = publicPaths.stream()
                .map(path -> "^" + contextPath + path)
                .map(Pattern::compile)
                .collect(toList());
        logger.info("initialized {} with public patterns: {}", OidcAuthenticationFilter.class.getName(), publicPatterns);
    }

    private String contextPath(FilterConfig filterConfig) {
        String contextPath = filterConfig.getServletContext().getContextPath();
        if (contextPath == null || contextPath.length() <= 1) {
            contextPath = "";
        }
        return contextPath;
    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException {
        HttpServletRequest httpServletRequest = (HttpServletRequest) request;
        HttpServletResponse httpServletResponse = (HttpServletResponse) response;

        if (isPublic(httpServletRequest)) {
            chain.doFilter(request, response);
            return;
        }

        StringBuilder errors = new StringBuilder();
        for (OidcAuthenticator authenticator : oidcAuthenticators) {

            Optional<String> token = authenticator.findIdToken(httpServletRequest);

            if (token.isPresent()) {
                try {
                    JWT jwtToken = JWTParser.parse(token.get());

                    // Skip this authenticator if the audience is not matching
                    if (!hasMatchingAudience(jwtToken, authenticator.config.clientId)) {
                        errors
                                .append("Token validation failed, audience mismatch. JwtAudience: ")
                                .append(jwtToken.getJWTClaimsSet().getAudience())
                                .append(" AuthenticatorAudience: ")
                                .append(authenticator.config.clientId)
                                .append("\n");
                        continue;
                    }

                    Optional<String> refreshedIdToken = refreshIdTokenIfNecessary(jwtToken, authenticator, httpServletRequest);

                    if (refreshedIdToken.isPresent()) {
                        jwtToken = JWTParser.parse(refreshedIdToken.get());

                        String idTokenCookieName = authenticator.config.idTokenCookieName;
                        addNewIdTokenCookie(idTokenCookieName, jwtToken, httpServletRequest, httpServletResponse);
                    }

                    authenticator.tokenValidator.validate(jwtToken);

                    SsoToken ssoToken = SsoToken.oidcToken(jwtToken.getParsedString(), jwtToken.getJWTClaimsSet().getClaims());
                    Subject subject = new Subject(
                            TokenUtils.getUid(jwtToken, authenticator.config.identType),
                            authenticator.config.identType, ssoToken
                    );

                    SubjectHandler.withSubject(subject, () -> chain.doFilter(request, response));
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
        logger.warn(errors.toString());
        httpServletResponse.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
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

    public boolean isPublic(HttpServletRequest httpServletRequest) {
        return publicPatterns.stream().anyMatch(p -> p.matcher(httpServletRequest.getRequestURI()).matches());
    }

    @Override
    public void destroy() {}

}
