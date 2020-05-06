package no.nav.common.oidc;

import no.nav.brukerdialog.security.domain.IdentType;
import no.nav.common.oidc.auth.OidcAuthenticationFilter;
import no.nav.common.oidc.auth.OidcAuthenticator;
import no.nav.common.oidc.auth.OidcAuthenticatorConfig;
import no.nav.testconfig.security.JwtTestTokenIssuer;
import no.nav.testconfig.security.JwtTestTokenIssuerConfig;
import no.nav.testconfig.security.OidcProviderTestRule;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.util.SocketUtils;

import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;

import static java.util.Collections.singletonList;
import static no.nav.common.oidc.Constants.*;
import static org.assertj.core.api.Assertions.assertThat;
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
    public OidcProviderTestRule azureAdOidcProviderRule = new OidcProviderTestRule(SocketUtils.findAvailableTcpPort(), azureAdIssuerConfig);

    @Rule
    public OidcProviderTestRule openAMOidcProviderRule = new OidcProviderTestRule(SocketUtils.findAvailableTcpPort(), openAMIssuerConfig);


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
    public void isPublic() {
        OidcAuthenticationFilter authenticationFilter = new OidcAuthenticationFilter(
                Collections.emptyList(),
                singletonList("/public.*")
        );

        authenticationFilter.init(config("/abc"));
        assertThat(authenticationFilter.isPublic(request("/public/selftest"))).isFalse();
        assertThat(authenticationFilter.isPublic(request("/abc/api/def/public/selftest"))).isFalse();
        assertThat(authenticationFilter.isPublic(request("/abc/public/selftest"))).isTrue();

        authenticationFilter.init(config("/"));
        assertThat(authenticationFilter.isPublic(request("/api/def/public/selftest"))).isFalse();
        assertThat(authenticationFilter.isPublic(request("/public/selftest"))).isTrue();

        authenticationFilter.init(config(null));
        assertThat(authenticationFilter.isPublic(request("/api/def/public/selftest"))).isFalse();
        assertThat(authenticationFilter.isPublic(request("/public/selftest"))).isTrue();
    }

    @Test
    public void returns401IfMissingToken() throws IOException, ServletException {
        OidcAuthenticationFilter authenticationFilter = new OidcAuthenticationFilter(
                singletonList(OidcAuthenticator.fromConfig(azureAdAuthenticatorConfig)),
                singletonList("/public.*")
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
                singletonList(OidcAuthenticator.fromConfig(azureAdAuthenticatorConfig)),
                singletonList("/public.*")
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
                singletonList(OidcAuthenticator.fromConfig(azureAdAuthenticatorConfig)),
                singletonList("/public.*")
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

        verify(servletRequest, atLeastOnce()).getCookies(); // Make sure that we got past the public path check
        verify(servletResponse, never()).setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        verify(filterChain, times(1)).doFilter(servletRequest, servletResponse);
    }

    @Test
    public void authorizedRequestIsForwardedWithMultipleProviders() throws IOException, ServletException {
        OidcAuthenticationFilter authenticationFilter = new OidcAuthenticationFilter(
                Arrays.asList(
                        OidcAuthenticator.fromConfig(azureAdAuthenticatorConfig),
                        OidcAuthenticator.fromConfig(openAMAuthenticatorConfig)
                ),
                singletonList("/public.*")
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

        verify(servletRequest, atLeastOnce()).getCookies(); // Make sure that we got past the public path check
        verify(servletResponse, never()).setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        verify(filterChain, times(1)).doFilter(servletRequest, servletResponse);
    }

    @Test
    public void shouldNotRefreshTokenWhenNotExpired() throws IOException, ServletException {
        OidcAuthenticationFilter authenticationFilter = new OidcAuthenticationFilter(
                singletonList(OidcAuthenticator.fromConfig(azureAdAuthenticatorConfig)),
                singletonList("/public.*")
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
        verify(servletRequest, atLeastOnce()).getCookies(); // Make sure that we got past the public path check
        verify(servletResponse, never()).setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        verify(filterChain, times(1)).doFilter(servletRequest, servletResponse);
    }

    @Test
    public void shouldRefreshTokenWhenSoonToBeExpired() throws IOException, ServletException {
        OidcAuthenticationFilter authenticationFilter = new OidcAuthenticationFilter(
                singletonList(OidcAuthenticator.fromConfig(azureAdAuthenticatorConfig)),
                singletonList("/public.*")
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
        verify(servletRequest, atLeastOnce()).getCookies(); // Make sure that we got past the public path check
        verify(servletResponse, never()).setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        verify(filterChain, times(1)).doFilter(servletRequest, servletResponse);
    }

    @Test
    public void shouldKeepDomainAndPathWhenRefreshingToken() throws IOException, ServletException {
        OidcAuthenticationFilter authenticationFilter = new OidcAuthenticationFilter(
                singletonList(OidcAuthenticator.fromConfig(azureAdAuthenticatorConfig)),
                singletonList("/public.*")
        );

        JwtTestTokenIssuer.Claims claims = new JwtTestTokenIssuer.Claims("me");
        long threeMinutesFuture = (System.currentTimeMillis() + (1000 * 60 * 3)) / 1000;
        claims.setClaim("exp", threeMinutesFuture);
        String token = azureAdOidcProviderRule.getToken(claims);

        HttpServletResponse servletResponse = mock(HttpServletResponse.class);
        FilterChain filterChain = mock(FilterChain.class);
        HttpServletRequest servletRequest = request("/hello");

        Cookie azureAdCookie = new Cookie(azureAdAuthenticatorConfig.idTokenCookieName, token);
        azureAdCookie.setDomain("test.localhost");
        azureAdCookie.setPath("/custompath");

        when(servletRequest.getServerName()).thenReturn("test.local");
        when(servletRequest.getCookies()).thenReturn(new Cookie[]{
                azureAdCookie,
                new Cookie(REFRESH_TOKEN_COOKIE_NAME, "my-refresh-token")
        });

        authenticationFilter.init(config("/abc"));

        authenticationFilter.doFilter(servletRequest, servletResponse, filterChain);

        ArgumentCaptor<Cookie> setCookieCaptor = ArgumentCaptor.forClass(Cookie.class);
        verify(servletResponse, atLeastOnce()).addCookie(setCookieCaptor.capture());
        Cookie setCookie = setCookieCaptor.getValue();

        assertThat(setCookie.getDomain()).isEqualTo("test.localhost");
        assertThat(setCookie.getPath()).isEqualTo("/custompath");
    }

    @Test
    public void shouldNotRefreshTokenIfExpiredWhenMissingConfig() throws IOException, ServletException {
        OidcAuthenticationFilter authenticationFilter = new OidcAuthenticationFilter(
                singletonList(OidcAuthenticator.fromConfig(openAMAuthenticatorConfig)),
                singletonList("/public.*")
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
        verify(servletRequest, atLeastOnce()).getCookies(); // Make sure that we got past the public path check
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
