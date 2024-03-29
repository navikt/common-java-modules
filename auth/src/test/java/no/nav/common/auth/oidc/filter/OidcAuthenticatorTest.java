package no.nav.common.auth.oidc.filter;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import no.nav.common.auth.context.UserRole;
import org.junit.Test;

import java.util.Optional;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class OidcAuthenticatorTest {

    @Test
    public void findIdToken__skal_hente_fra_cookie() {
        OidcAuthenticatorConfig config = new OidcAuthenticatorConfig()
                .withUserRole(UserRole.INTERN)
                .withIdTokenCookieName("id_token")
                .withDiscoveryUrl("")
                .withClientId("");

        OidcAuthenticator authenticator = new OidcAuthenticator(null, config);

        HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getCookies()).thenReturn(new Cookie[]{new Cookie("id_token", "token")});

        Optional<String> token = authenticator.findIdToken(request);

        assertTrue(token.isPresent());
        assertEquals("token", token.get());
    }

    @Test
    public void findIdToken__skal_hente_fra_idTokenFinder_hvis_cookie_mangler() {
        OidcAuthenticatorConfig config = new OidcAuthenticatorConfig()
                .withUserRole(UserRole.INTERN)
                .withIdTokenCookieName("id_token")
                .withDiscoveryUrl("")
                .withClientId("");

        OidcAuthenticator authenticator = new OidcAuthenticator(null, config);

        HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getCookies()).thenReturn(new Cookie[]{});
        when(request.getHeader(eq("Authorization"))).thenReturn("Bearer token");

        Optional<String> token = authenticator.findIdToken(request);

        assertTrue(token.isPresent());
        assertEquals("token", token.get());
    }

}
