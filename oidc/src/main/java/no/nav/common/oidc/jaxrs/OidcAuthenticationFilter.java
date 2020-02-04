package no.nav.common.oidc.jaxrs;

import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.proc.BadJOSEException;
import com.nimbusds.jwt.JWT;
import com.nimbusds.jwt.JWTParser;
import no.nav.common.auth.SsoToken;
import no.nav.common.auth.Subject;
import no.nav.common.auth.SubjectHandler;
import no.nav.common.oidc.utils.TokenUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.*;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.text.ParseException;
import java.util.List;
import java.util.Optional;
import java.util.regex.Pattern;

import static java.util.stream.Collectors.toList;

public class OidcAuthenticationFilter implements Filter {
    private static final Logger logger = LoggerFactory.getLogger(OidcAuthenticationFilter.class);

    private final List<OidcAuthenticator> oidcAuthenticators;
    private final List<String> publicPaths;

    private List<Pattern> publicPatterns;

    public OidcAuthenticationFilter(List<OidcAuthenticator> oidcAuthenticators, List<String> publicPaths) {
        this.oidcAuthenticators = oidcAuthenticators;
        this.publicPaths = publicPaths;
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

        for (OidcAuthenticator authenticator : oidcAuthenticators) {

            Optional<String> token = authenticator.idTokenLocator.getToken(httpServletRequest);

            if (token.isPresent()) {
                try {
                    JWT jwtToken = JWTParser.parse(token.get());
                    authenticator.tokenValidator.validate(jwtToken);

                    SsoToken ssoToken = SsoToken.oidcToken(token.get(), jwtToken.getJWTClaimsSet().getClaims());
                    Subject subject = new Subject(
                            TokenUtils.getUid(jwtToken, authenticator.identType),
                            authenticator.identType, ssoToken
                    );

                    SubjectHandler.withSubject(subject, () -> chain.doFilter(request, response));
                    return;
                } catch (ParseException | JOSEException | BadJOSEException ignored) {}
            }

        }

        httpServletResponse.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
    }

    public boolean isPublic(HttpServletRequest httpServletRequest) {
        return publicPatterns.stream().anyMatch(p -> p.matcher(httpServletRequest.getRequestURI()).matches());
    }

    @Override
    public void destroy() {}

}
