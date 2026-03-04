package no.nav.common.auth.oidc.filter;

import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.proc.BadJOSEException;
import com.nimbusds.jwt.JWT;
import com.nimbusds.jwt.JWTParser;
import com.nimbusds.openid.connect.sdk.validators.BadJWTExceptions;
import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.FilterConfig;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import no.nav.common.auth.context.AuthContext;
import no.nav.common.auth.context.AuthContextHolderThreadLocal;
import no.nav.common.auth.context.UserRole;
import no.nav.common.auth.oidc.TokenRefreshClient;
import no.nav.common.auth.oidc.UserRoleNullException;
import no.nav.common.auth.utils.JavaxCookieUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.text.ParseException;
import java.util.Date;
import java.util.List;
import java.util.Optional;

import static no.nav.common.auth.utils.CookieUtils.dateToCookieMaxAge;
import static no.nav.common.auth.utils.TokenUtils.expiresWithin;
import static no.nav.common.auth.utils.TokenUtils.hasMatchingAudience;
import static no.nav.common.auth.utils.TokenUtils.hasMatchingIssuer;

@Deprecated
public class JavaxOidcAuthenticationFilter implements Filter {
    private static final Logger logger = LoggerFactory.getLogger(JavaxOidcAuthenticationFilter.class);

    // Check if the token is about to expire within the next 5 minutes
    private static final long CHECK_EXPIRES_WITHIN = 1000 * 60 * 5;

    private final List<JavaxOidcAuthenticator> oidcAuthenticators;

    private final TokenRefreshClient tokenRefreshClient;

    public JavaxOidcAuthenticationFilter(List<JavaxOidcAuthenticator> oidcAuthenticators) {
        this.oidcAuthenticators = oidcAuthenticators;
        tokenRefreshClient = new TokenRefreshClient();
    }

    @Override
    public void init(FilterConfig filterConfig) {}

    @Override
    public void doFilter(ServletRequest servletRequest, ServletResponse servletResponse, FilterChain chain) {
        HttpServletRequest request = (HttpServletRequest) servletRequest;
        HttpServletResponse response = (HttpServletResponse) servletResponse;

        for (JavaxOidcAuthenticator authenticator : oidcAuthenticators) {

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

                        addNewIdTokenCookie(authenticator.config, jwtToken, request, response);
                    }

                    authenticator.tokenValidator.validate(jwtToken);

                    UserRole userRole = authenticator.config.userRoleResolver.resolve(jwtToken.getJWTClaimsSet());

                    if (userRole == null) {
                        throw new UserRoleNullException();
                    }

                    AuthContext authContext = new AuthContext(userRole, jwtToken);

                    AuthContextHolderThreadLocal.instance().withContext(authContext, () -> chain.doFilter(servletRequest, servletResponse));
                    return;
                } catch ( ParseException | JOSEException | BadJOSEException exception) {
                    if (exception == BadJWTExceptions.EXPIRED_EXCEPTION) {
                        logger.info("Token validation failed", exception);
                    } else {
                        logger.error("Token validation failed", exception);
                    }
                } catch (UserRoleNullException e) {
                    logger.error("User roll is null");
                }
            }
        }

        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
    }

    private void addNewIdTokenCookie(
            JavaxOidcAuthenticatorConfig oidcConfig, JWT jwtToken, HttpServletRequest request, HttpServletResponse response
    ) throws ParseException {
        String cookieDomain = oidcConfig.refreshedCookieDomain != null ? oidcConfig.refreshedCookieDomain : JavaxCookieUtils.cookieDomain(request);
        String cookiePath = oidcConfig.refreshedCookiePath != null ? oidcConfig.refreshedCookiePath : "/";
        Date cookieExpiration = jwtToken.getJWTClaimsSet().getExpirationTime();

        Cookie newIdCookie = JavaxCookieUtils.createCookie(
                oidcConfig.idTokenCookieName,
                jwtToken.getParsedString(),
                cookieDomain,
                cookiePath,
                dateToCookieMaxAge(cookieExpiration),
                request.isSecure()
        );

        response.addCookie(newIdCookie);
    }

    private Optional<String> refreshIdTokenIfNecessary(JWT token, JavaxOidcAuthenticator authenticator, HttpServletRequest request) {
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
