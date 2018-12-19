package no.nav.brukerdialog.security.jaspic;

import lombok.SneakyThrows;
import no.nav.brukerdialog.security.domain.IdentType;
import no.nav.brukerdialog.security.jwks.CacheMissAction;
import no.nav.brukerdialog.security.oidc.OidcTokenValidator;
import no.nav.brukerdialog.security.oidc.OidcTokenValidatorResult;
import no.nav.brukerdialog.security.oidc.provider.OidcProvider;
import no.nav.common.auth.Subject;
import no.nav.common.auth.TestSubjectUtils;
import no.nav.json.JsonUtils;
import org.jose4j.jwt.JwtClaims;
import org.junit.Before;
import org.junit.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import static java.util.Arrays.asList;
import static java.util.Optional.of;
import static no.nav.brukerdialog.security.jwks.CacheMissAction.NO_REFRESH;
import static no.nav.brukerdialog.security.jwks.CacheMissAction.REFRESH;
import static no.nav.common.auth.SsoToken.Type.OIDC;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

public class OidcAuthModuleTest {

    private static final IdentType IDENT_TYPE = IdentType.values()[0];

    private HttpServletRequest httpServletRequest = new MockHttpServletRequest();
    private HttpServletResponse httpServletResponse = new MockHttpServletResponse();
    private OidcProvider provider1 = oidcProvider();
    private OidcProvider provider2 = oidcProvider();
    private OidcProvider provider3 = oidcProvider();
    private OidcTokenValidator oidcTokenValidator = mock(OidcTokenValidator.class);

    private OidcAuthModule oidcAuthModule = new OidcAuthModule(asList(provider1, provider2, provider3), oidcTokenValidator);

    @Before
    public void setup() {
        when(oidcTokenValidator.validate(anyString(), any(), any())).thenReturn(OidcTokenValidatorResult.invalid("invalid by default"));
    }

    @Test
    public void authenticate__no_matching_token__empty() {
        assertThat(oidcAuthModule.authenticate(httpServletRequest, httpServletResponse)).isEmpty();
    }

    @Test
    public void authenticate__matching_token__returns_subject() {
        Subject subject = testSubject("token3");
        mockValidSubjectForProvider(subject, provider3, NO_REFRESH);

        assertThat(oidcAuthModule.authenticate(httpServletRequest, httpServletResponse)).hasValue(subject);
    }

    @Test
    public void authenticate__key_rotation__refresh_key_cache_and_return_subject() {
        Subject subject = testSubject("token3");
        mockValidSubjectForProvider(subject, provider3, REFRESH);

        assertThat(oidcAuthModule.authenticate(httpServletRequest, httpServletResponse)).hasValue(subject);
    }

    @Test
    public void authenticate__matching_token_in_cache__no_refresh_of_key_caches() {
        when(provider1.getToken(httpServletRequest)).thenReturn(of("1"));
        when(provider2.getToken(httpServletRequest)).thenReturn(of("2"));
        when(provider3.getToken(httpServletRequest)).thenReturn(of("3"));

        assertThat(oidcAuthModule.authenticate(httpServletRequest, httpServletResponse)).isEmpty();

        verify(provider1, never()).getVerificationKey(any(), eq(REFRESH));
        verify(provider2, never()).getVerificationKey(any(), eq(REFRESH));
        verify(provider3, never()).getVerificationKey(any(), eq(REFRESH));
    }

    @Test
    public void authenticate__ignore_failing_providers() {
        Subject subject = testSubject("token3");
        mockValidSubjectForProvider(subject, provider3, NO_REFRESH);

        when(provider1.getToken(httpServletRequest)).thenThrow(Throwable.class);
        when(provider2.getToken(httpServletRequest)).thenThrow(Throwable.class);

        assertThat(oidcAuthModule.authenticate(httpServletRequest, httpServletResponse)).hasValue(subject);
    }

    @SneakyThrows
    private void mockValidSubjectForProvider(Subject subject, OidcProvider oidcProvider, CacheMissAction noRefresh) {
        String oidcToken = subject.getSsoToken(OIDC).get();

        when(oidcProvider.getToken(httpServletRequest)).thenReturn(of(oidcToken));

        JwtClaims jwtClaims = JwtClaims.parse(JsonUtils.toJson(subject.getSsoToken().getAttributes()));
        OidcTokenValidatorResult oidcTokenValidatorResult = OidcTokenValidatorResult.valid(jwtClaims);
        when(oidcTokenValidator.validate(oidcToken, oidcProvider, noRefresh)).thenReturn(oidcTokenValidatorResult);
    }

    private Subject testSubject(String token) {
        String uid = "test-subject";
        JwtClaims jwtClaims = new JwtClaims();
        jwtClaims.setSubject(uid);
        jwtClaims.setExpirationTimeMinutesInTheFuture(600);
        return TestSubjectUtils.builder()
                .uid(uid)
                .identType(IDENT_TYPE)
                .token(token)
                .tokenType(OIDC)
                .attributes(jwtClaims.getClaimsMap())
                .build();
    }

    private OidcProvider oidcProvider() {
        OidcProvider oidcProvider = mock(OidcProvider.class);
        when(oidcProvider.getIdentType(anyString())).thenReturn(IDENT_TYPE);
        return oidcProvider;
    }

}