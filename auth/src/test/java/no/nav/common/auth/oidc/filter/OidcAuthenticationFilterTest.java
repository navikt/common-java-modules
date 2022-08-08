package no.nav.common.auth.oidc.filter;

import no.nav.common.auth.context.AuthContextHolderThreadLocal;
import no.nav.common.auth.context.UserRole;
import no.nav.common.auth.test_provider.JwtTestTokenIssuer;
import no.nav.common.auth.test_provider.JwtTestTokenIssuerConfig;
import no.nav.common.auth.test_provider.OidcProviderTestRule;
import no.nav.common.auth.utils.CookieUtils;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.ArgumentCaptor;

import javax.servlet.*;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;

import static java.util.Collections.singletonList;
import static no.nav.common.auth.Constants.*;
import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.*;

public class OidcAuthenticationFilterTest {

    private final static String NAIS_STS_ID = "oidc-provider-test-rule-nais-sts";
    private final static String AZURE_AD_ID = "oidc-provider-test-rule-aad";
    private final static String OPEN_AM_ID = "oidc-provider-test-rule-aad";

    private final static JwtTestTokenIssuerConfig naisStsIssuerConfig = JwtTestTokenIssuerConfig.builder()
            .id(NAIS_STS_ID)
            .issuer(NAIS_STS_ID)
            .audience(NAIS_STS_ID)
            .build();

    private final static JwtTestTokenIssuerConfig azureAdIssuerConfig = JwtTestTokenIssuerConfig.builder()
            .id(AZURE_AD_ID)
            .issuer(AZURE_AD_ID)
            .audience(AZURE_AD_ID)
            .build();

    private final static JwtTestTokenIssuerConfig openAMIssuerConfig = JwtTestTokenIssuerConfig.builder()
            .id(OPEN_AM_ID)
            .issuer(OPEN_AM_ID)
            .audience(OPEN_AM_ID)
            .build();

    @Rule
    public OidcProviderTestRule naisStsOidcProviderRule = new OidcProviderTestRule(naisStsIssuerConfig);

    @Rule
    public OidcProviderTestRule azureAdOidcProviderRule = new OidcProviderTestRule(azureAdIssuerConfig);

    @Rule
    public OidcProviderTestRule openAMOidcProviderRule = new OidcProviderTestRule(openAMIssuerConfig);


    private OidcAuthenticatorConfig naisStsAuthenticatorConfig;

    private OidcAuthenticatorConfig azureAdAuthenticatorConfig;

    private OidcAuthenticatorConfig azureAdAuthenticatorConfigCookieOverride;

    private OidcAuthenticatorConfig openAMAuthenticatorConfig;

    @Before
    public void before() {
        naisStsAuthenticatorConfig = new OidcAuthenticatorConfig()
                .withDiscoveryUrl(naisStsOidcProviderRule.getDiscoveryUri())
                .withClientIds(List.of("srvveilarbtest", "srvveilarbdemo"))
                .withUserRole(UserRole.SYSTEM);

        azureAdAuthenticatorConfig = new OidcAuthenticatorConfig()
                .withDiscoveryUrl(azureAdOidcProviderRule.getDiscoveryUri())
                .withClientId(azureAdOidcProviderRule.getAudience())
                .withUserRole(UserRole.INTERN)
                .withRefreshUrl(azureAdOidcProviderRule.getRefreshUri())
                .withIdTokenCookieName(AZURE_AD_ID_TOKEN_COOKIE_NAME)
                .withRefreshTokenCookieName(REFRESH_TOKEN_COOKIE_NAME);

        azureAdAuthenticatorConfigCookieOverride = new OidcAuthenticatorConfig()
                .withDiscoveryUrl(azureAdOidcProviderRule.getDiscoveryUri())
                .withClientId(azureAdOidcProviderRule.getAudience())
                .withUserRole(UserRole.INTERN)
                .withRefreshUrl(azureAdOidcProviderRule.getRefreshUri())
                .withIdTokenCookieName(AZURE_AD_ID_TOKEN_COOKIE_NAME)
                .withRefreshTokenCookieName(REFRESH_TOKEN_COOKIE_NAME)
                .withRefreshedCookieDomain("overridden.local")
                .withRefreshedCookiePath("/overriddenpath");

        openAMAuthenticatorConfig = new OidcAuthenticatorConfig()
                .withDiscoveryUrl(openAMOidcProviderRule.getDiscoveryUri())
                .withClientId(openAMOidcProviderRule.getAudience())
                .withIdTokenCookieName(OPEN_AM_ID_TOKEN_COOKIE_NAME)
                .withUserRole(UserRole.INTERN);
    }

    @Test
    public void should_set_auth_context() throws IOException, ServletException {
        OidcAuthenticationFilter authenticationFilter = new OidcAuthenticationFilter(
                singletonList(OidcAuthenticator.fromConfig(naisStsAuthenticatorConfig))
        );

        authenticationFilter.init(config("/abc"));

        String srvveilarbtestToken = naisStsOidcProviderRule.getToken(
                new JwtTestTokenIssuer.Claims("srvveilarbtest")
                        .setClaim("aud", List.of(NAIS_STS_ID, "srvveilarbtest"))
                        .setClaim("azp", "srvveilarbtest")
        );

        HttpServletRequest servletRequest = request("/hello");
        HttpServletResponse servletResponse = mock(HttpServletResponse.class);

        // Cannot spy on lambdas
        FilterChain filterChain = spy(new FilterChain() {
            @Override
            public void doFilter(ServletRequest request, ServletResponse response) {
                assertEquals(srvveilarbtestToken, AuthContextHolderThreadLocal.instance().requireIdTokenString());
                assertEquals(UserRole.SYSTEM, AuthContextHolderThreadLocal.instance().requireRole());
            }
        });

        when(servletRequest.getHeader("Authorization")).thenReturn("Bearer " + srvveilarbtestToken);
        when(servletRequest.getCookies()).thenReturn(new Cookie[]{});

        authenticationFilter.doFilter(servletRequest, servletResponse, filterChain);

        verify(servletResponse, never()).setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        verify(filterChain, times(1)).doFilter(servletRequest, servletResponse);
    }

    @Test
    public void srvveilarbtestIsAuthorized() throws IOException, ServletException {
        OidcAuthenticationFilter authenticationFilter = new OidcAuthenticationFilter(
                singletonList(OidcAuthenticator.fromConfig(naisStsAuthenticatorConfig))
        );

        authenticationFilter.init(config("/abc"));

        HttpServletRequest servletRequest = request("/hello");
        HttpServletResponse servletResponse = mock(HttpServletResponse.class);
        FilterChain filterChain = mock(FilterChain.class);

        String srvveilarbtestToken = naisStsOidcProviderRule.getToken(
                new JwtTestTokenIssuer.Claims("srvveilarbtest")
                        .setClaim("aud", List.of(NAIS_STS_ID, "srvveilarbtest"))
                        .setClaim("azp", "srvveilarbtest")
        );

        when(servletRequest.getHeader("Authorization")).thenReturn("Bearer " + srvveilarbtestToken);

        authenticationFilter.doFilter(servletRequest, servletResponse, filterChain);

        verify(servletResponse, never()).setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        verify(filterChain, times(1)).doFilter(servletRequest, servletResponse);
    }

    @Test
    public void srvveilarbdemoIsAuthorized() throws IOException, ServletException {
        OidcAuthenticationFilter authenticationFilter = new OidcAuthenticationFilter(
                singletonList(OidcAuthenticator.fromConfig(naisStsAuthenticatorConfig))
        );

        authenticationFilter.init(config("/abc"));

        HttpServletRequest servletRequest = request("/hello");
        HttpServletResponse servletResponse = mock(HttpServletResponse.class);
        FilterChain filterChain = mock(FilterChain.class);

        String srvveilarbdemoToken = naisStsOidcProviderRule.getToken(
                new JwtTestTokenIssuer.Claims("srvveilarbdemo")
                        .setClaim("aud", List.of(NAIS_STS_ID, "srvveilarbdemo"))
                        .setClaim("azp", "srvveilarbdemo")
        );

        when(servletRequest.getHeader("Authorization")).thenReturn("Bearer " + srvveilarbdemoToken);

        authenticationFilter.doFilter(servletRequest, servletResponse, filterChain);

        verify(servletResponse, never()).setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        verify(filterChain, times(1)).doFilter(servletRequest, servletResponse);
    }

    @Test
    public void srvunknownIsNotAuthorized() {
        OidcAuthenticationFilter authenticationFilter = new OidcAuthenticationFilter(
                singletonList(OidcAuthenticator.fromConfig(naisStsAuthenticatorConfig))
        );

        authenticationFilter.init(config("/abc"));

        HttpServletRequest servletRequest = request("/hello");
        HttpServletResponse servletResponse = mock(HttpServletResponse.class);
        FilterChain filterChain = mock(FilterChain.class);

        String srvunknownToken = naisStsOidcProviderRule.getToken(
                new JwtTestTokenIssuer.Claims("srvunknown")
                        .setClaim("aud", List.of(NAIS_STS_ID, "srvunknown"))
                        .setClaim("azp", "srvunknown")
        );

        when(servletRequest.getHeader("Authorization")).thenReturn("Bearer " + srvunknownToken);

        authenticationFilter.doFilter(servletRequest, servletResponse, filterChain);

        verify(servletResponse, times(1)).setStatus(HttpServletResponse.SC_UNAUTHORIZED);
    }

    @Test
    public void returns401IfMissingToken() {
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
    public void returns401IfWrongToken() {
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
    public void authorizedRequestIsForwardedWithMultipleAuthenticators() throws IOException, ServletException {
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
    public void shouldRefreshWithCorrectCookieDomainAndPath() throws IOException, ServletException {
        OidcAuthenticationFilter authenticationFilter = new OidcAuthenticationFilter(
                singletonList(OidcAuthenticator.fromConfig(azureAdAuthenticatorConfigCookieOverride))
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
                CookieUtils.createCookie(azureAdAuthenticatorConfigCookieOverride.idTokenCookieName, token, "overridden.local", "/overriddenpath", 0, false),
                CookieUtils.createCookie(REFRESH_TOKEN_COOKIE_NAME, "my-refresh-token", "overridden.local", "/overriddenpath", 0, false)
        });

        authenticationFilter.init(config("/abc"));

        authenticationFilter.doFilter(servletRequest, servletResponse, filterChain);

        ArgumentCaptor<Cookie> cookieCapture = ArgumentCaptor.forClass(Cookie.class);
        verify(servletResponse, atLeastOnce()).addCookie(cookieCapture.capture());
        verify(servletResponse, never()).setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        verify(filterChain, times(1)).doFilter(servletRequest, servletResponse);

        Cookie cookie = cookieCapture.getValue();
        assertEquals("isso-idtoken", cookie.getName());
        assertEquals("overridden.local", cookie.getDomain());
        assertEquals("/overriddenpath", cookie.getPath());
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

    @Test
    public void userRoleIsNull_returnsUnauthorized() throws IOException, ServletException {
        OidcAuthenticatorConfig config = new OidcAuthenticatorConfig()
                .withDiscoveryUrl(azureAdAuthenticatorConfig.discoveryUrl)
                .withClientIds(azureAdAuthenticatorConfig.clientIds)
                .withUserRoleResolver(_jwtClaims -> null)
                .withRefreshUrl(azureAdAuthenticatorConfig.refreshUrl)
                .withIdTokenCookieName(azureAdAuthenticatorConfig.idTokenCookieName)
                .withRefreshTokenCookieName(azureAdAuthenticatorConfig.refreshTokenCookieName);

        OidcAuthenticationFilter authenticationFilter = new OidcAuthenticationFilter(
                singletonList(OidcAuthenticator.fromConfig(config))
        );

        JwtTestTokenIssuer.Claims claims = new JwtTestTokenIssuer.Claims("me");
        String token = azureAdOidcProviderRule.getToken(claims);

        HttpServletResponse servletResponse = mock(HttpServletResponse.class);
        FilterChain filterChain = mock(FilterChain.class);
        HttpServletRequest servletRequest = request("/hello");

        when(servletRequest.getCookies()).thenReturn(new Cookie[]{
                new Cookie(config.idTokenCookieName, token)
        });

        authenticationFilter.init(config("/abc"));

        authenticationFilter.doFilter(servletRequest, servletResponse, filterChain);

        verify(servletResponse, times(1)).setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        verify(filterChain, never()).doFilter(servletRequest, servletResponse);
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