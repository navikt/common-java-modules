package no.nav.apiapp.security;

import no.nav.apiapp.feil.IngenTilgang;
import no.nav.brukerdialog.security.context.SubjectRule;
import no.nav.brukerdialog.security.domain.IdentType;
import no.nav.common.auth.SsoToken;
import no.nav.common.auth.Subject;
import no.nav.sbl.dialogarena.common.abac.pep.AbacPersonId;
import no.nav.sbl.dialogarena.common.abac.pep.Pep;
import no.nav.sbl.dialogarena.common.abac.pep.RequestData;
import no.nav.sbl.dialogarena.common.abac.pep.cef.CefEventContext;
import no.nav.sbl.dialogarena.common.abac.pep.cef.CefEventResource;
import no.nav.sbl.dialogarena.common.abac.pep.domain.ResourceType;
import no.nav.sbl.dialogarena.common.abac.pep.domain.response.BiasedDecisionResponse;
import no.nav.sbl.dialogarena.common.abac.pep.domain.response.Decision;
import no.nav.sbl.dialogarena.common.abac.pep.domain.response.XacmlResponse;
import no.nav.sbl.dialogarena.test.junit.SystemPropertiesRule;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import javax.servlet.http.HttpServletRequest;
import java.util.HashMap;

import static no.nav.log.LogFilter.PREFERRED_NAV_CALL_ID_HEADER_NAME;
import static no.nav.sbl.dialogarena.common.abac.pep.domain.request.Action.ActionId.READ;
import static no.nav.sbl.dialogarena.common.abac.pep.domain.request.Action.ActionId.WRITE;
import static no.nav.sbl.util.EnvironmentUtils.APP_NAME_PROPERTY_NAME;
import static no.nav.sbl.util.EnvironmentUtils.requireApplicationName;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

public class PepClientTest {

    private static final AbacPersonId FNR = AbacPersonId.fnr("fnr");
    private static final String APPLICATION_DOMAIN = "test";
    private static final ResourceType RESOURCE_TYPE = ResourceType.values()[0];
    private BiasedDecisionResponse PERMIT = new BiasedDecisionResponse(Decision.Permit, new XacmlResponse());
    private BiasedDecisionResponse DENY = new BiasedDecisionResponse(Decision.Deny, new XacmlResponse());

    private final Pep pep = mock(Pep.class);
    private final PepClient pepClient = new PepClient(pep, APPLICATION_DOMAIN, RESOURCE_TYPE);
    private final HttpServletRequest httpServletRequest = mock(HttpServletRequest.class);

    @Rule
    public SystemPropertiesRule systemPropertiesRule = new SystemPropertiesRule();

    @Rule
    public SubjectRule subjectRule = new SubjectRule();
    private final String subjectUid = "subject_uid";

    @Before
    public void setup() {
        when(pep.nyRequest()).thenReturn(new RequestData());
        systemPropertiesRule.setProperty(APP_NAME_PROPERTY_NAME, "testapp");
        subjectRule.setSubject(new Subject(subjectUid, IdentType.InternBruker, SsoToken.oidcToken("token", new HashMap<>())));

        setupRequestContext();
    }

    private void setupRequestContext() {
        when(httpServletRequest.getHeader(PREFERRED_NAV_CALL_ID_HEADER_NAME)).thenReturn("call_id_header");
        when(httpServletRequest.getMethod()).thenReturn("GET");
        when(httpServletRequest.getRequestURI()).thenReturn("/some/path");
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(httpServletRequest));
    }

    @Test(expected = IngenTilgang.class)
    public void sjekkLeseTilgangTilFnr() {
        CefEventContext expectedCefEventContext = buildExpectedCefEventContext(CefEventResource.personId(FNR));

        when(pep.harInnloggetBrukerTilgangTilPerson(
                eq(FNR), eq(APPLICATION_DOMAIN), eq(READ), eq(RESOURCE_TYPE), eq(expectedCefEventContext))
        ).thenReturn(DENY);

        pepClient.sjekkLesetilgangTilFnr("fnr");
    }

    @Test(expected = IngenTilgang.class)
    public void sjekkLeseTilgangTilFnr_uten_respons() {
        pepClient.sjekkLesetilgangTilFnr("fnr");
    }

    @Test
    public void sjekkLeseTilgangTilFnr_har_tilgang() {
        CefEventContext expectedCefEventContext = buildExpectedCefEventContext(CefEventResource.personId(FNR));

        when(pep.harInnloggetBrukerTilgangTilPerson(
                eq(FNR), eq(APPLICATION_DOMAIN), eq(READ), eq(RESOURCE_TYPE), eq(expectedCefEventContext))
        ).thenReturn(PERMIT);

        assertThat(pepClient.sjekkLesetilgangTilFnr(FNR.getId())).isEqualTo(FNR.getId());
    }

    @Test(expected = IngenTilgang.class)
    public void sjekkSkriveTilgangTilFnr() {
        CefEventContext expectedCefEventContext = buildExpectedCefEventContext(CefEventResource.personId(FNR));

        when(pep.harInnloggetBrukerTilgangTilPerson(
                eq(FNR), eq(APPLICATION_DOMAIN), eq(WRITE), eq(RESOURCE_TYPE), eq(expectedCefEventContext))
        ).thenReturn(DENY);

        pepClient.sjekkSkrivetilgangTilFnr("fnr");
    }

    @Test(expected = IngenTilgang.class)
    public void sjekkSkriveTilgangTilFnr_uten_respons() {
        pepClient.sjekkSkrivetilgangTilFnr("fnr");
    }

    @Test
    public void sjekkSkriveTilgangTilFnr_har_tilgang() {
        CefEventContext expectedCefEventContext = buildExpectedCefEventContext(CefEventResource.personId(FNR));

        when(pep.harInnloggetBrukerTilgangTilPerson(
                eq(FNR), eq(APPLICATION_DOMAIN), eq(WRITE), eq(RESOURCE_TYPE), eq(expectedCefEventContext))
        ).thenReturn(PERMIT);

        assertThat(pepClient.sjekkSkrivetilgangTilFnr("fnr")).isEqualTo(FNR.getId());
    }

    @Test
    public void harTilgangTilEnhet_tilgang() {
        when(pep.harTilgang(any(RequestData.class), any())).thenReturn(PERMIT);
        assertTrue(pepClient.harTilgangTilEnhet("enhet"));
    }

    @Test
    public void harTilgangTilEnhet_ingen_tilgang() {
        CefEventContext expectedCefEventContext = buildExpectedCefEventContext(CefEventResource.enhetId("enhet"));

        when(pep.harTilgang(any(RequestData.class), eq(expectedCefEventContext))).thenReturn(DENY);
        assertFalse(pepClient.harTilgangTilEnhet("enhet"));
    }

    @Test
    public void harTilgangTilEnhet_enhet_null_konverteres_til_tom_streng() {
        CefEventContext expectedCefEventContext = buildExpectedCefEventContext(CefEventResource.enhetId(null));

        when(pep.harTilgang(any(RequestData.class), eq(expectedCefEventContext))).thenReturn(DENY);
        assertFalse(pepClient.harTilgangTilEnhet(null));
        verify(pep).harTilgang((RequestData) argThat(x -> ((RequestData) x).getEnhet().equals("")), any());
    }

    private CefEventContext buildExpectedCefEventContext(CefEventResource resource) {
        return CefEventContext.builder()
                .applicationName(requireApplicationName())
                .callId(httpServletRequest.getHeader(PREFERRED_NAV_CALL_ID_HEADER_NAME))
                .requestMethod(httpServletRequest.getMethod())
                .requestPath(httpServletRequest.getRequestURI())
                .resource(resource)
                .subjectId(subjectUid)
                .build();
    }
}
