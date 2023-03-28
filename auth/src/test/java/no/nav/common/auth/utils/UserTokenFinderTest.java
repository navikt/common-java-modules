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

public class UserTokenFinderTest {

    private final static String OPENAM_ID = "oidc-provider-test-rule-openam";

    private final static JwtTestTokenIssuerConfig openAmIssuerConfig = JwtTestTokenIssuerConfig.builder()
            .id(OPENAM_ID)
            .issuer(OPENAM_ID)
            .audience(OPENAM_ID)
            .build();

    @Rule
    public OidcProviderTestRule openAmOidcProviderRule = new OidcProviderTestRule(openAmIssuerConfig);

    @Test
    public void should_empty_token_for_service_user() {
        String token = openAmOidcProviderRule.getToken(new JwtTestTokenIssuer.Claims("srvveilarbtest"));

        HttpServletRequest servletRequest = mock(HttpServletRequest.class);
        when(servletRequest.getHeader("Authorization")).thenReturn("Bearer " + token);

        Optional<String> maybeToken = new UserTokenFinder().findToken(servletRequest);

        assertTrue(maybeToken.isEmpty());
    }

    @Test
    public void should_return_empty_for_service_user() {
        String token = openAmOidcProviderRule.getToken(new JwtTestTokenIssuer.Claims("Z123456"));

        HttpServletRequest servletRequest =  mock(HttpServletRequest.class);
        when(servletRequest.getHeader("Authorization")).thenReturn("Bearer " + token);

        Optional<String> maybeToken = new UserTokenFinder().findToken(servletRequest);

        assertTrue(maybeToken.isPresent());
    }

    @Test
    public void should_return_empty_for_missing_auth_header() {
        HttpServletRequest servletRequest =  mock(HttpServletRequest.class);
        when(servletRequest.getHeader("Authorization")).thenReturn(null);

        Optional<String> maybeToken = new ServiceUserTokenFinder().findToken(servletRequest);

        assertTrue(maybeToken.isEmpty());
    }

}
