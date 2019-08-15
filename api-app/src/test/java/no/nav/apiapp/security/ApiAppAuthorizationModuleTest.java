package no.nav.apiapp.security;

import no.nav.brukerdialog.security.domain.IdentType;
import no.nav.common.auth.AuthorizationModule;
import no.nav.common.auth.SecurityLevel;
import no.nav.common.auth.Subject;
import org.junit.Test;

import javax.servlet.http.HttpServletRequest;
import java.util.*;

import static no.nav.brukerdialog.security.oidc.OidcTokenUtils.SECURITY_LEVEL_ATTRIBUTE;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static no.nav.common.auth.SsoToken.oidcToken;
import static org.assertj.core.api.Assertions.assertThat;

public class ApiAppAuthorizationModuleTest {

    private HttpServletRequest request = mock(HttpServletRequest.class);
    private AuthorizationModule customAuthorization = mock(AuthorizationModule.class);

    @Test
    public void authorized__equal_security_level__without_custom_authorization() {
        assertThat(authModule(SecurityLevel.Level3).authorized(createSubjectEksternBruker(SecurityLevel.Level3), request)).isTrue();
    }

    @Test
    public void authorized__high_security_level__without_custom_authorization() {
        assertThat(authModule(SecurityLevel.Level3).authorized(createSubjectEksternBruker(SecurityLevel.Level4), request)).isTrue();
    }

    @Test
    public void authorized__highest_security_level__without_custom_authorization() {
        Arrays.stream(SecurityLevel.values()).forEach(securityLevel ->
                assertThat(authModule(securityLevel).authorized(createSubjectEksternBruker(SecurityLevel.Level4), request)).isTrue());
    }

    @Test
    public void authorized__handles_no_security_level__without_custom_authorization() {
        assertThat(authModule(null).authorized(createSubjectEksternBruker(SecurityLevel.Ukjent), request)).isTrue();
        assertThat(authModule(null).authorized(createSubjectInternBruker(SecurityLevel.Ukjent), request)).isTrue();
    }

    @Test
    public void unauthorized__low_security_level__without_custom_authorization() {
        assertThat(authModule(SecurityLevel.Level3).authorized(createSubjectEksternBruker(SecurityLevel.Level2), request)).isFalse();
    }

    @Test
    public void autorized__non_external_ident_types__without_custom_authorization() {
        Arrays.stream(IdentType.values()).filter(identType -> !identType.equals(IdentType.EksternBruker))
                .forEach(identType ->
                        assertThat(authModule(SecurityLevel.Level4).authorized(createSubject(identType), request)).isTrue());
    }

    @Test
    public void autorized__non_external_ident_types__no_security_level_without_custom_authorization() {
        Arrays.stream(IdentType.values()).filter(identType -> !identType.equals(IdentType.EksternBruker))
                .forEach(identType ->
                        assertThat(authModule(null).authorized(createSubject(identType), request)).isTrue());
    }

    @Test
    public void authorized__authorized_custom_authorization() {
        when(customAuthorization.authorized(any(), any())).thenReturn(true);
        assertThat(authModuleWithCustom(SecurityLevel.Level3).authorized(createSubjectEksternBruker(SecurityLevel.Level3), request)).isTrue();
    }

    @Test
    public void authorized__unauthorized_custom_authorization() {
        when(customAuthorization.authorized(any(), any())).thenReturn(false);
        assertThat(authModuleWithCustom(SecurityLevel.Level3).authorized(createSubjectEksternBruker(SecurityLevel.Level3), request)).isFalse();
    }

    @Test
    public void unauthorized__authorized_custom_authorization() {
        when(customAuthorization.authorized(any(), any())).thenReturn(true);
        assertThat(authModuleWithCustom(SecurityLevel.Level4).authorized(createSubjectEksternBruker(SecurityLevel.Level3), request)).isFalse();
    }

    @Test
    public void unauthorized__unauthorized_custom_authorization() {
        when(customAuthorization.authorized(any(), any())).thenReturn(false);
        assertThat(authModuleWithCustom(SecurityLevel.Level4).authorized(createSubjectEksternBruker(SecurityLevel.Level3), request)).isFalse();
    }

    @Test
    public void authorized__non_external_ident_types__authorized_custom_authorization() {
        when(customAuthorization.authorized(any(), any())).thenReturn(true);
        Arrays.stream(IdentType.values()).filter(identType -> !identType.equals(IdentType.EksternBruker))
                .forEach(identType ->
                        assertThat(authModuleWithCustom(SecurityLevel.Level4).authorized(createSubject(identType), request)).isTrue());
    }

    @Test
    public void unauthorized__non_external_ident_types__unauthorized_custom_authorization() {
        when(customAuthorization.authorized(any(), any())).thenReturn(false);
        Arrays.stream(IdentType.values()).filter(identType -> !identType.equals(IdentType.EksternBruker))
                .forEach(identType ->
                        assertThat(authModuleWithCustom(SecurityLevel.Level4).authorized(createSubject(identType), request)).isFalse());
    }

    @Test
    public void custom_security_levels_for_paths_applied_only_to_external_users() {
        HashMap<SecurityLevel, List<String>> securityLevelForPaths = new HashMap<>();
        securityLevelForPaths.put(SecurityLevel.Level4, Arrays.asList("level4"));
        securityLevelForPaths.put(SecurityLevel.Level2, Arrays.asList("level2"));
        securityLevelForPaths.put(SecurityLevel.Level1, Arrays.asList("level1"));
        ApiAppAuthorizationModule apiAppAuthorizationModule = authModuleWithPaths(SecurityLevel.Level3, securityLevelForPaths);

        when(request.getPathInfo()).thenReturn("/level4");
        assertThat(apiAppAuthorizationModule.authorized(createSubjectEksternBruker(SecurityLevel.Level3), request)).isFalse();
        assertThat(apiAppAuthorizationModule.authorized(createSubjectInternBruker(SecurityLevel.Level3), request)).isTrue();

        when(request.getPathInfo()).thenReturn("/level4");
        assertThat(apiAppAuthorizationModule.authorized(createSubjectEksternBruker(SecurityLevel.Level4), request)).isTrue();
        assertThat(apiAppAuthorizationModule.authorized(createSubjectInternBruker(SecurityLevel.Level4), request)).isTrue();

        when(request.getPathInfo()).thenReturn("/level3");
        assertThat(apiAppAuthorizationModule.authorized(createSubjectEksternBruker(SecurityLevel.Level3), request)).isTrue();
        assertThat(apiAppAuthorizationModule.authorized(createSubjectEksternBruker(SecurityLevel.Level2), request)).isFalse();
        assertThat(apiAppAuthorizationModule.authorized(createSubjectInternBruker(SecurityLevel.Level3), request)).isTrue();
        assertThat(apiAppAuthorizationModule.authorized(createSubjectInternBruker(SecurityLevel.Level2), request)).isTrue();

        when(request.getPathInfo()).thenReturn("/level3/");
        assertThat(apiAppAuthorizationModule.authorized(createSubjectEksternBruker(SecurityLevel.Level3), request)).isTrue();
        assertThat(apiAppAuthorizationModule.authorized(createSubjectInternBruker(SecurityLevel.Level3), request)).isTrue();

        when(request.getPathInfo()).thenReturn("/level3/ok");
        assertThat(apiAppAuthorizationModule.authorized(createSubjectEksternBruker(SecurityLevel.Level3), request)).isTrue();
        assertThat(apiAppAuthorizationModule.authorized(createSubjectInternBruker(SecurityLevel.Level3), request)).isTrue();

        when(request.getPathInfo()).thenReturn("/level2");
        assertThat(apiAppAuthorizationModule.authorized(createSubjectEksternBruker(SecurityLevel.Level2), request)).isTrue();
        assertThat(apiAppAuthorizationModule.authorized(createSubjectInternBruker(SecurityLevel.Level2), request)).isTrue();

        when(request.getPathInfo()).thenReturn("/level2");
        assertThat(apiAppAuthorizationModule.authorized(createSubjectEksternBruker(SecurityLevel.Level3), request)).isTrue();
        assertThat(apiAppAuthorizationModule.authorized(createSubjectInternBruker(SecurityLevel.Level3), request)).isTrue();

        when(request.getPathInfo()).thenReturn("/level2");
        assertThat(apiAppAuthorizationModule.authorized(createSubjectEksternBruker(SecurityLevel.Level1), request)).isFalse();
        assertThat(apiAppAuthorizationModule.authorized(createSubjectInternBruker(SecurityLevel.Level1), request)).isTrue();

        when(request.getPathInfo()).thenReturn("/level2/nested/path/ok");
        assertThat(apiAppAuthorizationModule.authorized(createSubjectEksternBruker(SecurityLevel.Level2), request)).isTrue();
        assertThat(apiAppAuthorizationModule.authorized(createSubjectInternBruker(SecurityLevel.Level2), request)).isTrue();

        when(request.getPathInfo()).thenReturn("/level1");
        assertThat(apiAppAuthorizationModule.authorized(createSubjectEksternBruker(SecurityLevel.Level1), request)).isTrue();
        assertThat(apiAppAuthorizationModule.authorized(createSubjectInternBruker(SecurityLevel.Level1), request)).isTrue();
    }

    @Test(expected = IllegalStateException.class)
    public void ambiguous_security_level_for_path_not_allowed() {
        HashMap<SecurityLevel, List<String>> securityLevelForPaths = new HashMap<>();
        securityLevelForPaths.put(SecurityLevel.Level4, Arrays.asList("level"));
        securityLevelForPaths.put(SecurityLevel.Level1, Arrays.asList("level"));
        authModuleWithPaths(SecurityLevel.Level3, securityLevelForPaths);
    }

    @Test
    public void base_path_for_security_level_should_not_be_empty_nested_or_contain_query_params() {
        assertThat(isValidBasePathForSecurityLevel("")).isFalse();
        assertThat(isValidBasePathForSecurityLevel("level")).isTrue();
        assertThat(isValidBasePathForSecurityLevel("/level?a=1&b=2")).isFalse();
        assertThat(isValidBasePathForSecurityLevel("/level")).isFalse();
        assertThat(isValidBasePathForSecurityLevel("level/abc")).isFalse();
        assertThat(isValidBasePathForSecurityLevel("level?")).isFalse();
        assertThat(isValidBasePathForSecurityLevel("level&")).isFalse();
        assertThat(isValidBasePathForSecurityLevel("level=")).isFalse();
    }

    private boolean isValidBasePathForSecurityLevel(String securityLevelForBasePath) {
        try {
            HashMap<SecurityLevel, List<String>> securityLevelForPaths = new HashMap<>();
            securityLevelForPaths.put(SecurityLevel.Level4, Arrays.asList(securityLevelForBasePath));
            authModuleWithPaths(null, securityLevelForPaths);
        } catch (IllegalStateException e) {
            return e.getMessage().equals("Ambiguous security level for " + securityLevelForBasePath);
        }
        return true;
    }

    private ApiAppAuthorizationModule authModule(SecurityLevel securityLevel) {
        return new ApiAppAuthorizationModule(null, securityLevel, Collections.emptyMap());
    }

    private ApiAppAuthorizationModule authModuleWithCustom(SecurityLevel securityLevel) {
        return new ApiAppAuthorizationModule(customAuthorization, securityLevel, Collections.emptyMap());
    }

    private ApiAppAuthorizationModule authModuleWithPaths(SecurityLevel securityLevel, Map<SecurityLevel, List<String>> securityLevelForPaths) {
        return new ApiAppAuthorizationModule(null, securityLevel, securityLevelForPaths);
    }


    private Subject createSubjectEksternBruker(SecurityLevel securityLevel) {
        Map<String, Object> attributes = new HashMap<>();
        attributes.put(SECURITY_LEVEL_ATTRIBUTE, securityLevel.name());
        return new Subject("test-ident", IdentType.EksternBruker, oidcToken("test-token", attributes));
    }

    private Subject createSubjectInternBruker(SecurityLevel securityLevel) {
        Map<String, Object> attributes = new HashMap<>();
        attributes.put(SECURITY_LEVEL_ATTRIBUTE, securityLevel.name());
        return new Subject("test-ident", IdentType.InternBruker, oidcToken("test-token", attributes));
    }

    private Subject createSubject(IdentType identType) {
        Map<String, Object> attributes = new HashMap<>();
        return new Subject("test-ident", identType, oidcToken("test-token", attributes));
    }
}
