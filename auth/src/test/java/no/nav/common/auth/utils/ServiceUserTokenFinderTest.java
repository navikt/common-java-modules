package no.nav.common.auth.utils;

import jakarta.servlet.http.HttpServletRequest;
import no.nav.common.auth.test_provider.JwtTestTokenIssuer;
import no.nav.common.auth.test_provider.JwtTestTokenIssuerConfig;
import no.nav.common.auth.test_provider.OidcProviderTestRule;
import org.junit.Rule;
import org.junit.Test;

import java.util.Optional;

import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class ServiceUserTokenFinderTest {

    private final static String NAIS_STS_ID = "oidc-provider-test-rule-nais-sts";

    private final static JwtTestTokenIssuerConfig naisStsIssuerConfig = JwtTestTokenIssuerConfig.builder()
            .id(NAIS_STS_ID)
            .issuer(NAIS_STS_ID)
            .audience(NAIS_STS_ID)
            .build();

    @Rule
    public OidcProviderTestRule naisStsOidcProviderRule = new OidcProviderTestRule(naisStsIssuerConfig);

    @Test
    public void should_return_token_for_service_user() {
        String token = naisStsOidcProviderRule.getToken(new JwtTestTokenIssuer.Claims("srvveilarbtest"));

        HttpServletRequest servletRequest = mock(HttpServletRequest.class);
        when(servletRequest.getHeader("Authorization")).thenReturn("Bearer " + token);

        Optional<String> maybeToken = new ServiceUserTokenFinder().findToken(servletRequest);

        assertTrue(maybeToken.isPresent());
    }

    @Test
    public void should_return_empty_for_non_service_user() {
        String token = naisStsOidcProviderRule.getToken(new JwtTestTokenIssuer.Claims("some-user"));

        HttpServletRequest servletRequest =  mock(HttpServletRequest.class);
        when(servletRequest.getHeader("Authorization")).thenReturn("Bearer " + token);

        Optional<String> maybeToken = new ServiceUserTokenFinder().findToken(servletRequest);

        assertTrue(maybeToken.isEmpty());
    }

    @Test
    public void should_return_empty_for_missing_auth_header() {
        HttpServletRequest servletRequest =  mock(HttpServletRequest.class);
        when(servletRequest.getHeader("Authorization")).thenReturn(null);

        Optional<String> maybeToken = new ServiceUserTokenFinder().findToken(servletRequest);

        assertTrue(maybeToken.isEmpty());
    }

}
