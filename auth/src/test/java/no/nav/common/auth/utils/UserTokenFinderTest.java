package no.nav.common.auth.utils;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import jakarta.servlet.http.HttpServletRequest;
import java.util.Optional;
import no.nav.common.auth.test_provider.JwtTestTokenIssuer;
import no.nav.common.auth.test_provider.JwtTestTokenIssuerConfig;
import no.nav.common.auth.test_provider.OidcProviderTestRule;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

public class UserTokenFinderTest {

    private static final String OPENAM_ID = "oidc-provider-test-rule-openam";

    private static final JwtTestTokenIssuerConfig openAmIssuerConfig = JwtTestTokenIssuerConfig.builder()
            .id(OPENAM_ID)
            .issuer(OPENAM_ID)
            .audience(OPENAM_ID)
            .build();

    @RegisterExtension
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

        HttpServletRequest servletRequest = mock(HttpServletRequest.class);
        when(servletRequest.getHeader("Authorization")).thenReturn("Bearer " + token);

        Optional<String> maybeToken = new UserTokenFinder().findToken(servletRequest);

        assertTrue(maybeToken.isPresent());
    }

    @Test
    public void should_return_empty_for_missing_auth_header() {
        HttpServletRequest servletRequest = mock(HttpServletRequest.class);
        when(servletRequest.getHeader("Authorization")).thenReturn(null);

        Optional<String> maybeToken = new UserTokenFinder().findToken(servletRequest);

        assertTrue(maybeToken.isEmpty());
    }
}
