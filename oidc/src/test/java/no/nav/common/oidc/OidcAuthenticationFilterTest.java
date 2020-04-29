package no.nav.common.oidc;

import no.nav.common.auth.IdentType;
import no.nav.common.oidc.auth.OidcAuthenticationFilter;
import no.nav.common.oidc.auth.OidcAuthenticator;
import no.nav.common.oidc.auth.OidcAuthenticatorConfig;
import no.nav.common.oidc.test_provider.JwtTestTokenIssuer;
import no.nav.common.oidc.test_provider.JwtTestTokenIssuerConfig;
import no.nav.common.oidc.test_provider.OidcProviderTestRule;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Arrays;

import static java.util.Collections.singletonList;
import static no.nav.common.oidc.Constants.*;
import static org.mockito.Mockito.*;

public class OidcAuthenticationFilterTest {

    private final static JwtTestTokenIssuerConfig azureAdIssuerConfig = JwtTestTokenIssuerConfig.builder()
            .id("oidc-provider-test-rule-aad")
            .issuer("oidc-provider-test-rule-aad")
            .audience("oidc-provider-test-rule-aad")
            .build();

    private final static JwtTestTokenIssuerConfig openAMIssuerConfig = JwtTestTokenIssuerConfig.builder()
            .id("oidc-provider-test-rule-openam")
            .issuer("oidc-provider-test-rule-openam")
            .audience("oidc-provider-test-rule-openam")
            .build();

    @Rule
    public OidcProviderTestRule azureAdOidcProviderRule = new OidcProviderTestRule(azureAdIssuerConfig);

    @Rule
    public OidcProviderTestRule openAMOidcProviderRule = new OidcProviderTestRule(openAMIssuerConfig);


    private OidcAuthenticatorConfig azureAdAuthenticatorConfig;

    private OidcAuthenticatorConfig openAMAuthenticatorConfig;


    @Before
    public void before() {
        azureAdAuthenticatorConfig = new OidcAuthenticatorConfig()
                .withDiscoveryUrl(azureAdOidcProviderRule.getDiscoveryUri())
                .withClientId(azureAdOidcProviderRule.getAudience())
                .withIdentType(IdentType.InternBruker)
                .withRefreshUrl(azureAdOidcProviderRule.getRefreshUri())
                .withIdTokenCookieName(AZURE_AD_ID_TOKEN_COOKIE_NAME)
                .withRefreshTokenCookieName(REFRESH_TOKEN_COOKIE_NAME);

        openAMAuthenticatorConfig = new OidcAuthenticatorConfig()
                .withDiscoveryUrl(openAMOidcProviderRule.getDiscoveryUri())
                .withClientId(openAMOidcProviderRule.getAudience())
                .withIdTokenCookieName(OPEN_AM_ID_TOKEN_COOKIE_NAME)
                .withIdentType(IdentType.InternBruker);
    }

    @Test
    public void returns401IfMissingToken() throws IOException, ServletException {
        OidcAuthenticationFilter authenticationFilter = new OidcAuthenticationFilter(
                singletonList(OidcAuthenticator.fromConfig(azureAdAuthenticatorConfig))
        );

        authenticationFilter.init(config("/abc"));

        HttpServletRequest servletRequest = request("/hello");
        HttpServletResponse servletResponse = mock(HttpServletResponse.class);
        FilterChain filterChain = mock(FilterChain.class);

        when(servletRequest.getCookies()).thenReturn(new Cookie[]{});

        authenticationFilter.doFilter(servletRequest, servletResponse, filterChain);

        verify(servletResponse).setStatus(HttpServletResponse.SC_UNAUTHORIZED);
    }

    @Test
    public void returns401IfWrongToken() throws IOException, ServletException {
        OidcAuthenticationFilter authenticationFilter = new OidcAuthenticationFilter(
                singletonList(OidcAuthenticator.fromConfig(azureAdAuthenticatorConfig))
        );

        authenticationFilter.init(config("/abc"));

        HttpServletRequest servletRequest = request("/hello");
        JwtTestTokenIssuer.Claims claims = new JwtTestTokenIssuer.Claims("me");
        when(servletRequest.getCookies()).thenReturn(new Cookie[]{
                new Cookie(azureAdAuthenticatorConfig.idTokenCookieName, openAMOidcProviderRule.getToken(claims))
        });

        HttpServletResponse servletResponse = mock(HttpServletResponse.class);
        FilterChain filterChain = mock(FilterChain.class);

        authenticationFilter.doFilter(servletRequest, servletResponse, filterChain);

        verify(servletResponse).setStatus(HttpServletResponse.SC_UNAUTHORIZED);
    }

    @Test
    public void authorizedRequestIsForwarded() throws IOException, ServletException {
        OidcAuthenticationFilter authenticationFilter = new OidcAuthenticationFilter(
                singletonList(OidcAuthenticator.fromConfig(azureAdAuthenticatorConfig))
        );

        JwtTestTokenIssuer.Claims claims = new JwtTestTokenIssuer.Claims("me");
        String token = azureAdOidcProviderRule.getToken(claims);

        HttpServletResponse servletResponse = mock(HttpServletResponse.class);
        FilterChain filterChain = mock(FilterChain.class);
        HttpServletRequest servletRequest = request("/hello");

        when(servletRequest.getCookies()).thenReturn(new Cookie[]{
                new Cookie(azureAdAuthenticatorConfig.idTokenCookieName, token)
        });

        authenticationFilter.init(config("/abc"));

        authenticationFilter.doFilter(servletRequest, servletResponse, filterChain);

        verify(servletResponse, never()).setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        verify(filterChain, times(1)).doFilter(servletRequest, servletResponse);
    }

    @Test
    public void authorizedRequestIsForwardedWithMultipleProviders() throws IOException, ServletException {
        OidcAuthenticationFilter authenticationFilter = new OidcAuthenticationFilter(
                Arrays.asList(
                        OidcAuthenticator.fromConfig(azureAdAuthenticatorConfig),
                        OidcAuthenticator.fromConfig(openAMAuthenticatorConfig)
                )
        );

        JwtTestTokenIssuer.Claims claims = new JwtTestTokenIssuer.Claims("me");
        String token = azureAdOidcProviderRule.getToken(claims);

        HttpServletResponse servletResponse = mock(HttpServletResponse.class);
        FilterChain filterChain = mock(FilterChain.class);
        HttpServletRequest servletRequest = request("/hello");

        when(servletRequest.getCookies()).thenReturn(new Cookie[]{
                new Cookie(azureAdAuthenticatorConfig.idTokenCookieName, token)
        });

        authenticationFilter.init(config("/abc"));

        authenticationFilter.doFilter(servletRequest, servletResponse, filterChain);

        verify(servletResponse, never()).setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        verify(filterChain, times(1)).doFilter(servletRequest, servletResponse);
    }

    @Test
    public void shouldNotRefreshTokenWhenNotExpired() throws IOException, ServletException {
        OidcAuthenticationFilter authenticationFilter = new OidcAuthenticationFilter(
                singletonList(OidcAuthenticator.fromConfig(azureAdAuthenticatorConfig))
        );

        JwtTestTokenIssuer.Claims claims = new JwtTestTokenIssuer.Claims("me");
        String token = azureAdOidcProviderRule.getToken(claims);

        HttpServletResponse servletResponse = mock(HttpServletResponse.class);
        FilterChain filterChain = mock(FilterChain.class);
        HttpServletRequest servletRequest = request("/hello");

        when(servletRequest.getCookies()).thenReturn(new Cookie[]{
                new Cookie(azureAdAuthenticatorConfig.idTokenCookieName, token)
        });

        authenticationFilter.init(config("/abc"));

        authenticationFilter.doFilter(servletRequest, servletResponse, filterChain);

        verify(servletResponse, never()).addCookie(any());
        verify(servletResponse, never()).setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        verify(filterChain, times(1)).doFilter(servletRequest, servletResponse);
    }

    @Test
    public void shouldRefreshTokenWhenSoonToBeExpired() throws IOException, ServletException {
        OidcAuthenticationFilter authenticationFilter = new OidcAuthenticationFilter(
                singletonList(OidcAuthenticator.fromConfig(azureAdAuthenticatorConfig))
        );

        JwtTestTokenIssuer.Claims claims = new JwtTestTokenIssuer.Claims("me");
        long threeMinutesFuture = (System.currentTimeMillis() + (1000 * 60 * 3)) / 1000;
        claims.setClaim("exp", threeMinutesFuture);
        String token = azureAdOidcProviderRule.getToken(claims);

        HttpServletResponse servletResponse = mock(HttpServletResponse.class);
        FilterChain filterChain = mock(FilterChain.class);
        HttpServletRequest servletRequest = request("/hello");

        when(servletRequest.getServerName()).thenReturn("test.local");
        when(servletRequest.getCookies()).thenReturn(new Cookie[]{
                new Cookie(azureAdAuthenticatorConfig.idTokenCookieName, token),
                new Cookie(REFRESH_TOKEN_COOKIE_NAME, "my-refresh-token")
        });

        authenticationFilter.init(config("/abc"));

        authenticationFilter.doFilter(servletRequest, servletResponse, filterChain);

        verify(servletResponse, atLeastOnce()).addCookie(any());
        verify(servletResponse, never()).setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        verify(filterChain, times(1)).doFilter(servletRequest, servletResponse);
    }

    @Test
    public void shouldNotRefreshTokenIfExpiredWhenMissingConfig() throws IOException, ServletException {
        OidcAuthenticationFilter authenticationFilter = new OidcAuthenticationFilter(
                singletonList(OidcAuthenticator.fromConfig(openAMAuthenticatorConfig))
        );

        JwtTestTokenIssuer.Claims claims = new JwtTestTokenIssuer.Claims("me");
        long threeMinutesFuture = (System.currentTimeMillis() + (1000 * 60 * 3)) / 1000;
        claims.setClaim("exp", threeMinutesFuture);
        String token = openAMOidcProviderRule.getToken(claims);

        HttpServletResponse servletResponse = mock(HttpServletResponse.class);
        FilterChain filterChain = mock(FilterChain.class);
        HttpServletRequest servletRequest = request("/hello");

        when(servletRequest.getServerName()).thenReturn("test.local");
        when(servletRequest.getCookies()).thenReturn(new Cookie[]{
                new Cookie(OPEN_AM_ID_TOKEN_COOKIE_NAME, token),
                new Cookie(REFRESH_TOKEN_COOKIE_NAME, "my-refresh-token")
        });

        authenticationFilter.init(config("/abc"));

        authenticationFilter.doFilter(servletRequest, servletResponse, filterChain);

        verify(servletResponse, never()).addCookie(any());
        verify(servletResponse, never()).setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        verify(filterChain, times(1)).doFilter(servletRequest, servletResponse);
    }

    private HttpServletRequest request(String requestPath) {
        HttpServletRequest httpServletRequest = mock(HttpServletRequest.class);
        when(httpServletRequest.getRequestURI()).thenReturn(requestPath);
        return httpServletRequest;
    }

    private FilterConfig config(String contextPath) {
        FilterConfig filterConfig = mock(FilterConfig.class);
        ServletContext servletContext = mock(ServletContext.class);
        when(servletContext.getContextPath()).thenReturn(contextPath);
        when(filterConfig.getServletContext()).thenReturn(servletContext);
        return filterConfig;
    }

}