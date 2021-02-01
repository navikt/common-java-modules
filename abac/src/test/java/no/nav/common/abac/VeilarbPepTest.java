package no.nav.common.abac;

import no.nav.common.abac.audit.AuditLogger;
import no.nav.common.abac.audit.AuditRequestInfo;
import no.nav.common.abac.audit.AuditRequestInfoSupplier;
import no.nav.common.abac.audit.SubjectProvider;
import no.nav.common.abac.cef.CefEvent;
import no.nav.common.abac.domain.request.XacmlRequest;
import no.nav.common.abac.domain.response.XacmlResponse;
import no.nav.common.health.HealthCheckResult;
import no.nav.common.test.junit.SystemPropertiesRule;
import no.nav.common.types.identer.EnhetId;
import no.nav.common.types.identer.Fnr;
import no.nav.common.types.identer.NavIdent;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.slf4j.Logger;

import static java.lang.String.format;
import static no.nav.common.abac.TestUtils.assertJsonEquals;
import static no.nav.common.abac.TestUtils.getContentFromJsonFile;
import static no.nav.common.abac.cef.CefEvent.Severity.INFO;
import static no.nav.common.abac.cef.CefEvent.Severity.WARN;
import static no.nav.common.abac.constants.AbacDomain.MODIA_DOMAIN;
import static no.nav.common.abac.constants.AbacDomain.VEILARB_DOMAIN;
import static no.nav.common.abac.constants.NavAttributter.*;
import static no.nav.common.abac.domain.request.ActionId.READ;
import static no.nav.common.abac.domain.response.Decision.Deny;
import static no.nav.common.abac.domain.response.Decision.Permit;
import static no.nav.common.utils.EnvironmentUtils.NAIS_APP_NAME_PROPERTY_NAME;
import static no.nav.common.utils.IdUtils.generateId;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.*;

public class VeilarbPepTest {

    private final static Fnr TEST_FNR = Fnr.of("12345678900");

    private final static String TEST_SRV_USERNAME = "test";
    private final static NavIdent TEST_VEILEDER_IDENT = NavIdent.of("Z1234");
    private final static EnhetId TEST_ENHET_ID = EnhetId.of("1234");
    private static final String TEST_OIDC_TOKEN = "abc.abc.abc";
    private final static String APPLICATION_NAME = "testapp";
    private static final long TIME = System.currentTimeMillis();
    private static final String CALL_ID = generateId();
    private static final String CONSUMER_ID = "ConsumingApplication";
    private static final String REQUEST_METHOD = "GET";
    private static final String REQUEST_PATH = "/some/path";
    private static final EnhetId ENHET = EnhetId.of("5678");
    private final static AuditRequestInfo AUDIT_REQUEST_INFO = new AuditRequestInfo(CALL_ID, CONSUMER_ID, REQUEST_METHOD, REQUEST_PATH, () -> true);

    private final Logger log = mock(Logger.class);
    private final SubjectProvider subjectProvider = mock(SubjectProvider.class);
    private final AuditLogger auditLogger = new AuditLogger(log, () -> TIME);
    private final AuditRequestInfoSupplier auditRequestInfoSupplier = () -> AUDIT_REQUEST_INFO;

    @Rule
    public SystemPropertiesRule systemPropertiesRule = new SystemPropertiesRule();

    @Before
    public void setup() {
        systemPropertiesRule.setProperty(NAIS_APP_NAME_PROPERTY_NAME, APPLICATION_NAME);
        when(subjectProvider.getSubjectFromToken(TEST_OIDC_TOKEN)).thenReturn(TEST_VEILEDER_IDENT.get());
    }

    private final AbacClient genericPermitClient = abacClientSpyWithResponseFromFile("xacmlresponse-generic-permit.json");
    private final AbacClient genericDenyClient = abacClientSpyWithResponseFromFile("xacmlresponse-generic-deny.json");

    @Test
    public void harVeilederTilgangTilEnhet__skal_lage_riktig_request() {
        VeilarbPep veilarbPep = new VeilarbPep(TEST_SRV_USERNAME, genericPermitClient, auditLogger, subjectProvider, auditRequestInfoSupplier);
        String expectedRequest = getContentFromJsonFile("xacmlrequest-harVeilederTilgangTilEnhet.json");
        ArgumentCaptor<String> captor = ArgumentCaptor.forClass(String.class);

        boolean tilgang = veilarbPep.harVeilederTilgangTilEnhet(TEST_VEILEDER_IDENT, TEST_ENHET_ID);

        assertTrue(tilgang);
        verify(genericPermitClient, times(1)).sendRawRequest(captor.capture());
        assertJsonEquals(expectedRequest, captor.getValue());
    }

    @Test
    public void harVeilederTilgangTilEnhet__skal_returnere_false_hvis_ikke_tilgang() {
        VeilarbPep veilarbPep = new VeilarbPep(TEST_SRV_USERNAME, genericDenyClient, auditLogger, subjectProvider, auditRequestInfoSupplier);
        boolean tilgang = veilarbPep.harVeilederTilgangTilEnhet(TEST_VEILEDER_IDENT, TEST_ENHET_ID);

        assertFalse(tilgang);
    }

    @Test
    public void harTilgangTilEnhet__skal_lage_riktig_request() {
        VeilarbPep veilarbPep = new VeilarbPep(TEST_SRV_USERNAME, genericPermitClient, auditLogger, subjectProvider, auditRequestInfoSupplier);
        String expectedRequest = getContentFromJsonFile("xacmlrequest-harTilgangTilEnhet.json");
        ArgumentCaptor<String> captor = ArgumentCaptor.forClass(String.class);

        boolean tilgang = veilarbPep.harTilgangTilEnhet(TEST_OIDC_TOKEN, TEST_ENHET_ID);

        assertTrue(tilgang);
        verify(genericPermitClient, times(1)).sendRawRequest(captor.capture());
        assertJsonEquals(expectedRequest, captor.getValue());
    }

    @Test
    public void harTilgangTilEnhet__skal_returnere_false_hvis_ikke_tilgang() {
        VeilarbPep veilarbPep = new VeilarbPep(TEST_SRV_USERNAME, genericDenyClient, auditLogger, subjectProvider, auditRequestInfoSupplier);
        boolean tilgang = veilarbPep.harTilgangTilEnhet(TEST_OIDC_TOKEN, TEST_ENHET_ID);

        assertFalse(tilgang);
    }

    @Test
    public void harTilgangTilEnhetMedSperre__skal_lage_riktig_request() {
        VeilarbPep veilarbPep = new VeilarbPep(TEST_SRV_USERNAME, genericPermitClient, auditLogger, subjectProvider, auditRequestInfoSupplier);
        String expectedRequest = getContentFromJsonFile("xacmlrequest-harTilgangTilEnhetMedSperre.json");
        ArgumentCaptor<String> captor = ArgumentCaptor.forClass(String.class);

        boolean tilgang = veilarbPep.harTilgangTilEnhetMedSperre(TEST_OIDC_TOKEN, TEST_ENHET_ID);

        assertTrue(tilgang);
        verify(genericPermitClient, times(1)).sendRawRequest(captor.capture());
        assertJsonEquals(expectedRequest, captor.getValue());
    }

    @Test
    public void harTilgangTilEnhetMedSperre__skal_returnere_false_hvis_ikke_tilgang() {
        VeilarbPep veilarbPep = new VeilarbPep(TEST_SRV_USERNAME, genericDenyClient, auditLogger, subjectProvider, auditRequestInfoSupplier);
        boolean tilgang = veilarbPep.harTilgangTilEnhetMedSperre(TEST_OIDC_TOKEN, TEST_ENHET_ID);

        assertFalse(tilgang);
    }

    @Test
    public void harVeilederTilgangTilPerson__skal_lage_riktig_request() {
        VeilarbPep veilarbPep = new VeilarbPep(TEST_SRV_USERNAME, genericPermitClient, auditLogger, subjectProvider, auditRequestInfoSupplier);
        String expectedRequest = getContentFromJsonFile("xacmlrequest-harVeilederTilgangTilPerson.json");
        ArgumentCaptor<String> captor = ArgumentCaptor.forClass(String.class);

        boolean tilgang = veilarbPep.harVeilederTilgangTilPerson(TEST_VEILEDER_IDENT, READ, TEST_FNR);

        assertTrue(tilgang);
        verify(genericPermitClient, times(1)).sendRawRequest(captor.capture());
        assertJsonEquals(expectedRequest, captor.getValue());
    }

    @Test
    public void harVeilederTilgangTilPerson__skal_returnere_false_hvis_ikke_tilgang() {
        VeilarbPep veilarbPep = new VeilarbPep(TEST_SRV_USERNAME, genericDenyClient, auditLogger, subjectProvider, auditRequestInfoSupplier);
        boolean tilgang = veilarbPep.harVeilederTilgangTilPerson(TEST_VEILEDER_IDENT, READ, TEST_FNR);

        assertFalse(tilgang);
    }


    @Test
    public void harTilgangTilPerson__skal_lage_riktig_request() {
        VeilarbPep veilarbPep = new VeilarbPep(TEST_SRV_USERNAME, genericPermitClient, auditLogger, subjectProvider, auditRequestInfoSupplier);
        String expectedRequest = getContentFromJsonFile("xacmlrequest-harTilgangTilPerson.json");
        ArgumentCaptor<String> captor = ArgumentCaptor.forClass(String.class);

        boolean tilgang = veilarbPep.harTilgangTilPerson(TEST_OIDC_TOKEN, READ, TEST_FNR);
        assertTrue(tilgang);

        verify(genericPermitClient, times(1)).sendRawRequest(captor.capture());
        assertJsonEquals(expectedRequest, captor.getValue());
    }

    @Test
    public void harTilgangTilPerson__skal_returnere_false_hvis_ikke_tilgang() {
        VeilarbPep veilarbPep = new VeilarbPep(TEST_SRV_USERNAME, genericDenyClient, auditLogger, subjectProvider, auditRequestInfoSupplier);
        boolean tilgang = veilarbPep.harTilgangTilPerson(TEST_OIDC_TOKEN, READ, TEST_FNR);

        assertFalse(tilgang);
    }


    @Test
    public void harVeilederTilgangTilKode6__skal_lage_riktig_request() {
        VeilarbPep veilarbPep = new VeilarbPep(TEST_SRV_USERNAME, genericPermitClient, auditLogger, subjectProvider, auditRequestInfoSupplier);
        String expectedRequest = getContentFromJsonFile("xacmlrequest-harVeilederTilgangTilKode6.json");
        ArgumentCaptor<String> captor = ArgumentCaptor.forClass(String.class);

        boolean tilgang = veilarbPep.harVeilederTilgangTilKode6(TEST_VEILEDER_IDENT);

        assertTrue(tilgang);
        verify(genericPermitClient, times(1)).sendRawRequest(captor.capture());
        assertJsonEquals(expectedRequest, captor.getValue());
    }

    @Test
    public void harVeilederTilgangTilKode6__skal_kaste_exception_hvis_ikke_tilgang() {
        VeilarbPep veilarbPep = new VeilarbPep(TEST_SRV_USERNAME, genericDenyClient, auditLogger, subjectProvider, auditRequestInfoSupplier);
        boolean tilgang = veilarbPep.harVeilederTilgangTilKode6(TEST_VEILEDER_IDENT);

        assertFalse(tilgang);
    }

    @Test
    public void sjekkVeilederTilgangTilKode7__skal_lage_riktig_request() {
        VeilarbPep veilarbPep = new VeilarbPep(TEST_SRV_USERNAME, genericPermitClient, auditLogger, subjectProvider, auditRequestInfoSupplier);
        String expectedRequest = getContentFromJsonFile("xacmlrequest-harVeilederTilgangTilKode7.json");
        ArgumentCaptor<String> captor = ArgumentCaptor.forClass(String.class);

        boolean tilgang = veilarbPep.harVeilederTilgangTilKode7(TEST_VEILEDER_IDENT);

        assertTrue(tilgang);
        verify(genericPermitClient, times(1)).sendRawRequest(captor.capture());
        assertJsonEquals(expectedRequest, captor.getValue());
    }

    @Test
    public void sjekkVeilederTilgangTilKode7__returnere_false_hvis_ikke_tilgang() {
        VeilarbPep veilarbPep = new VeilarbPep(TEST_SRV_USERNAME, genericDenyClient, auditLogger, subjectProvider, auditRequestInfoSupplier);
        boolean tilgang = veilarbPep.harVeilederTilgangTilKode7(TEST_VEILEDER_IDENT);

        assertFalse(tilgang);
    }

    @Test
    public void sjekkVeilederTilgangTilEgenAnsatt__skal_lage_riktig_request() {
        VeilarbPep veilarbPep = new VeilarbPep(TEST_SRV_USERNAME, genericPermitClient, auditLogger, subjectProvider, auditRequestInfoSupplier);
        String expectedRequest = getContentFromJsonFile("xacmlrequest-harVeilederTilgangTilEgenAnsatt.json");
        ArgumentCaptor<String> captor = ArgumentCaptor.forClass(String.class);

        boolean tilgang = veilarbPep.harVeilederTilgangTilEgenAnsatt(TEST_VEILEDER_IDENT);

        assertTrue(tilgang);
        verify(genericPermitClient, times(1)).sendRawRequest(captor.capture());
        assertJsonEquals(expectedRequest, captor.getValue());
    }

    @Test
    public void sjekkVeilederTilgangTilEgenAnsatt__returnere_false_hvis_ikke_tilgang() {
        VeilarbPep veilarbPep = new VeilarbPep(TEST_SRV_USERNAME, genericDenyClient, auditLogger, subjectProvider, auditRequestInfoSupplier);
        boolean tilgang = veilarbPep.harVeilederTilgangTilEgenAnsatt(TEST_VEILEDER_IDENT);

        assertFalse(tilgang);
    }

    @Test
    public void harVeilederTilgangTilEnhet__riktig_audit_log_for_permit() {
        VeilarbPep veilarbPep = new VeilarbPep(TEST_SRV_USERNAME, genericPermitClient, auditLogger, subjectProvider, auditRequestInfoSupplier);
        assertTrue(veilarbPep.harVeilederTilgangTilEnhet(TEST_VEILEDER_IDENT, ENHET));
        verify(log).info(eq(expectCefLogHeader(INFO) + expectCefLogAttributesEnhetPermit()));
    }

    @Test
    public void harVeilederTilgangTilEnhet__riktig_audit_log_for_deny() {
        VeilarbPep veilarbPep = new VeilarbPep(TEST_SRV_USERNAME, genericDenyClient, auditLogger, subjectProvider, auditRequestInfoSupplier);
        assertFalse(veilarbPep.harVeilederTilgangTilEnhet(TEST_VEILEDER_IDENT, ENHET));
        verify(log).info(eq(expectCefLogHeader(WARN) + expectCefLogAttributesEnhetDeny()));
    }

    @Test
    public void harVeilederTilgangTilPerson__riktig_audit_log_for_permit() {
        VeilarbPep veilarbPep = new VeilarbPep(TEST_SRV_USERNAME, genericPermitClient, auditLogger, subjectProvider, auditRequestInfoSupplier);
        assertTrue(veilarbPep.harVeilederTilgangTilPerson(TEST_VEILEDER_IDENT, READ, TEST_FNR));
        verify(log).info(eq(expectCefLogHeader(INFO) + expectCefLogAttributesPersonPermit()));
    }

    @Test
    public void harVeilederTilgangTilPerson__riktig_audit_log_for_deny() {
        VeilarbPep veilarbPep = new VeilarbPep(TEST_SRV_USERNAME, genericDenyClient, auditLogger, subjectProvider, auditRequestInfoSupplier);
        assertFalse(veilarbPep.harVeilederTilgangTilPerson(TEST_VEILEDER_IDENT, READ, TEST_FNR));
        verify(log).info(eq(expectCefLogHeader(WARN) + expectCefLogAttributesPersonDeny()));
    }

    @Test
    public void harTilgangTilPerson__riktig_audit_log_for_permit() {
        VeilarbPep veilarbPep = new VeilarbPep(TEST_SRV_USERNAME, genericPermitClient, auditLogger, subjectProvider, auditRequestInfoSupplier);
        assertTrue(veilarbPep.harTilgangTilPerson(TEST_OIDC_TOKEN, READ, TEST_FNR));
        verify(log).info(eq(expectCefLogHeader(INFO) + expectCefLogAttributesPersonPermit()));
    }

    @Test
    public void harTilgangTilPerson__riktig_audit_log_for_deny() {
        VeilarbPep veilarbPep = new VeilarbPep(TEST_SRV_USERNAME, genericDenyClient, auditLogger, subjectProvider, auditRequestInfoSupplier);
        assertFalse(veilarbPep.harTilgangTilPerson(TEST_OIDC_TOKEN, READ, TEST_FNR));
        verify(log).info(eq(expectCefLogHeader(WARN) + expectCefLogAttributesPersonDeny()));
    }

    @Test
    public void harVeilederTilgangTilKode6__riktig_audit_log_for_permit() {
        VeilarbPep veilarbPep = new VeilarbPep(TEST_SRV_USERNAME, genericPermitClient, auditLogger, subjectProvider, auditRequestInfoSupplier);
        assertTrue(veilarbPep.harVeilederTilgangTilKode6(TEST_VEILEDER_IDENT));
        verify(log).info(eq(expectCefLogHeader(INFO) + expectCefLogAttributesResourcePermit(SUBJECT_FELLES_HAR_TILGANG_KODE_6)));
    }

    @Test
    public void harVeilederTilgangTilKode6__riktig_audit_log_for_deny() {
        VeilarbPep veilarbPep = new VeilarbPep(TEST_SRV_USERNAME, genericDenyClient, auditLogger, subjectProvider, auditRequestInfoSupplier);
        assertFalse(veilarbPep.harVeilederTilgangTilKode6(TEST_VEILEDER_IDENT));
        verify(log).info(eq(expectCefLogHeader(WARN) + expectCefLogAttributesResourceDeny(SUBJECT_FELLES_HAR_TILGANG_KODE_6)));
    }

    @Test
    public void harVeilederTilgangTilKode7__riktig_audit_log_for_permit() {
        VeilarbPep veilarbPep = new VeilarbPep(TEST_SRV_USERNAME, genericPermitClient, auditLogger, subjectProvider, auditRequestInfoSupplier);
        assertTrue(veilarbPep.harVeilederTilgangTilKode7(TEST_VEILEDER_IDENT));
        verify(log).info(eq(expectCefLogHeader(INFO) + expectCefLogAttributesResourcePermit(SUBJECT_FELLES_HAR_TILGANG_KODE_7)));
    }

    @Test
    public void harVeilederTilgangTilKode7__riktig_audit_log_for_deny() {
        VeilarbPep veilarbPep = new VeilarbPep(TEST_SRV_USERNAME, genericDenyClient, auditLogger, subjectProvider, auditRequestInfoSupplier);
        assertFalse(veilarbPep.harVeilederTilgangTilKode7(TEST_VEILEDER_IDENT));
        verify(log).info(eq(expectCefLogHeader(WARN) + expectCefLogAttributesResourceDeny(SUBJECT_FELLES_HAR_TILGANG_KODE_7)));
    }

    @Test
    public void harVeilederTilgangTilEgenAnsatt__riktig_audit_log_for_permit() {
        VeilarbPep veilarbPep = new VeilarbPep(TEST_SRV_USERNAME, genericPermitClient, auditLogger, subjectProvider, auditRequestInfoSupplier);
        assertTrue(veilarbPep.harVeilederTilgangTilEgenAnsatt(TEST_VEILEDER_IDENT));
        verify(log).info(eq(expectCefLogHeader(INFO) + expectCefLogAttributesResourcePermit(SUBJECT_FELLES_HAR_TILGANG_EGEN_ANSATT)));
    }

    @Test
    public void harVeilederTilgangTilEgenAnsatt__riktig_audit_log_for_deny() {
        VeilarbPep veilarbPep = new VeilarbPep(TEST_SRV_USERNAME, genericDenyClient, auditLogger, subjectProvider, auditRequestInfoSupplier);
        assertFalse(veilarbPep.harVeilederTilgangTilEgenAnsatt(TEST_VEILEDER_IDENT));
        verify(log).info(eq(expectCefLogHeader(WARN) + expectCefLogAttributesResourceDeny(SUBJECT_FELLES_HAR_TILGANG_EGEN_ANSATT)));
    }

    @Test
    public void ingen_audit_log_dersom_request_info_er_null() {
        VeilarbPep veilarbPep = new VeilarbPep(TEST_SRV_USERNAME, genericPermitClient, auditLogger, subjectProvider, () -> null);
        assertTrue(veilarbPep.harTilgangTilPerson(TEST_OIDC_TOKEN, READ, TEST_FNR));
        verify(log, never()).info(any());
    }

    @Test
    public void ingen_audit_log_dersom_request_info_supplier_er_null() {
        VeilarbPep veilarbPep = new VeilarbPep(TEST_SRV_USERNAME, genericPermitClient, auditLogger, subjectProvider, null);
        assertTrue(veilarbPep.harTilgangTilPerson(TEST_OIDC_TOKEN, READ, TEST_FNR));
        verify(log, never()).info(any());
    }

    @Test
    public void ingen_audit_log_dersom_det_filtreres_bort() {
        AuditRequestInfo auditRequestInfo = new AuditRequestInfo(CALL_ID, CONSUMER_ID, REQUEST_METHOD, REQUEST_PATH, () -> false);
        VeilarbPep veilarbPep = new VeilarbPep(TEST_SRV_USERNAME, genericPermitClient, auditLogger, subjectProvider, () -> auditRequestInfo);
        assertTrue(veilarbPep.harTilgangTilPerson(TEST_OIDC_TOKEN, READ, TEST_FNR));
        verify(log, never()).info(any());
    }

    @Test
    public void harTilgangTilOppfolging__skal_lage_riktig_request() {
        VeilarbPep veilarbPep = new VeilarbPep(TEST_SRV_USERNAME, genericPermitClient, auditLogger, subjectProvider, auditRequestInfoSupplier);
        String expectedRequest = getContentFromJsonFile("xacmlrequest-harTilgangTilOppfolging.json");
        ArgumentCaptor<String> captor = ArgumentCaptor.forClass(String.class);

        assertTrue(veilarbPep.harTilgangTilOppfolging(TEST_OIDC_TOKEN));

        verify(genericPermitClient, times(1)).sendRawRequest(captor.capture());
        assertJsonEquals(expectedRequest, captor.getValue());
    }


    @Test
    public void harTilgangTilOppfolging__riktig_audit_log_for_permit() {
        VeilarbPep veilarbPep = new VeilarbPep(TEST_SRV_USERNAME, genericPermitClient, auditLogger, subjectProvider, auditRequestInfoSupplier);
        assertTrue(veilarbPep.harTilgangTilOppfolging(TEST_OIDC_TOKEN));
        verify(log).info(eq(expectCefLogHeader(INFO) + expectCefLogAttributesResourcePermit(RESOURCE_VEILARB)));
    }

    @Test
    public void harTilgangTilOppfolging__riktig_audit_log_for_deny() {
        VeilarbPep veilarbPep = new VeilarbPep(TEST_SRV_USERNAME, genericDenyClient, auditLogger, subjectProvider, auditRequestInfoSupplier);
        assertFalse(veilarbPep.harTilgangTilOppfolging(TEST_OIDC_TOKEN));
        verify(log).info(eq(expectCefLogHeader(WARN) + expectCefLogAttributesResourceDeny(RESOURCE_VEILARB)));
    }

    @Test
    public void harVeilederTilgangTilModia__skal_lage_riktig_request() {
        VeilarbPep veilarbPep = new VeilarbPep(TEST_SRV_USERNAME, genericPermitClient, auditLogger, subjectProvider, auditRequestInfoSupplier);
        String expectedRequest = getContentFromJsonFile("xacmlrequest-harVeilederTilgangTilModia.json");
        ArgumentCaptor<String> captor = ArgumentCaptor.forClass(String.class);

        assertTrue(veilarbPep.harVeilederTilgangTilModia(TEST_OIDC_TOKEN));

        verify(genericPermitClient, times(1)).sendRawRequest(captor.capture());
        assertJsonEquals(expectedRequest, captor.getValue());
    }

    @Test
    public void harVeilederTilgangTilModia__riktig_audit_log_for_permit() {
        VeilarbPep veilarbPep = new VeilarbPep(TEST_SRV_USERNAME, genericPermitClient, auditLogger, subjectProvider, auditRequestInfoSupplier);
        assertTrue(veilarbPep.harVeilederTilgangTilModia(TEST_OIDC_TOKEN));
        verify(log).info(eq(expectCefLogHeader(INFO) + expectCefLogAttributesModiaResourcePermit(RESOURCE_MODIA)));
    }

    @Test
    public void harVeilederTilgangTilModia__riktig_audit_log_for_deny() {
        VeilarbPep veilarbPep = new VeilarbPep(TEST_SRV_USERNAME, genericDenyClient, auditLogger, subjectProvider, auditRequestInfoSupplier);
        assertFalse(veilarbPep.harVeilederTilgangTilModia(TEST_OIDC_TOKEN));
        verify(log).info(eq(expectCefLogHeader(WARN) + expectCefLogAttributesModiaResourceDeny(RESOURCE_MODIA)));
    }

    private AbacClient abacClientSpyWithResponseFromFile(String fileName) {
        return spy(new AbacClient() {
            @Override
            public HealthCheckResult checkHealth() {
                return HealthCheckResult.healthy();
            }

            @Override
            public String sendRawRequest(String xacmlRequestJson) {
                return getContentFromJsonFile(fileName);
            }

            @Override
            public XacmlResponse sendRequest(XacmlRequest xacmlRequest) {
                return XacmlMapper.mapRawResponse(sendRawRequest(XacmlMapper.mapRequestToEntity(xacmlRequest)));
            }
        });
    }

    private String expectCefLogHeader(CefEvent.Severity severity) {
        return format("CEF:0|%s|Sporingslogg|1.0|audit:access|ABAC Sporingslogg|%s|", APPLICATION_NAME, severity);
    }

    private String expectCefLogAttributesEnhetPermit() {
        return "sproc=" + CALL_ID +
                " flexString1=" + Permit +
                " request=" + REQUEST_PATH +
                " act=" + READ.getId() +
                " cs2=" + ENHET +
                " requestContext=" + RESOURCE_FELLES_ENHET +
                " sourceServiceName=" + VEILARB_DOMAIN +
                " requestMethod=" + REQUEST_METHOD +
                " end=" + TIME +
                " flexString1Label=Decision" +
                " suid=" + TEST_VEILEDER_IDENT +
                " dproc=" + CONSUMER_ID;
    }

    private String expectCefLogAttributesEnhetDeny() {
        return "sproc=" + CALL_ID +
                " flexString2Label=deny_policy" +
                " request=" + REQUEST_PATH +
                " cs5Label=deny_rule" +
                " cs3=cause" +
                " cs2=" + ENHET +
                " cs5=deny_rule" +
                " requestMethod=" + REQUEST_METHOD +
                " suid=" + TEST_VEILEDER_IDENT +
                " dproc=" + CONSUMER_ID +
                " flexString1=" + Deny +
                " act=" + READ.getId() +
                " requestContext=" + RESOURCE_FELLES_ENHET +
                " sourceServiceName=" + VEILARB_DOMAIN +
                " cs3Label=deny_cause" +
                " end=" + TIME +
                " flexString1Label=Decision" +
                " flexString2=deny_policy";
    }

    private String expectCefLogAttributesPersonPermit() {
        return "sproc=" + CALL_ID +
                " flexString1=" + Permit +
                " request=" + REQUEST_PATH +
                " act=" + READ.getId() +
                " duid=" + TEST_FNR.get() +
                " requestContext=" + RESOURCE_FELLES_PERSON +
                " sourceServiceName=" + VEILARB_DOMAIN +
                " requestMethod=" + REQUEST_METHOD +
                " end=" + TIME +
                " flexString1Label=Decision" +
                " suid=" + TEST_VEILEDER_IDENT +
                " dproc=" + CONSUMER_ID;
    }

    private String expectCefLogAttributesPersonDeny() {
        return "sproc=" + CALL_ID +
                " flexString2Label=deny_policy" +
                " request=" + REQUEST_PATH +
                " cs5Label=deny_rule" +
                " cs3=cause" +
                " duid=" + TEST_FNR.get() +
                " cs5=deny_rule" +
                " requestMethod=" + REQUEST_METHOD +
                " suid=" + TEST_VEILEDER_IDENT +
                " dproc=" + CONSUMER_ID +
                " flexString1=" + Deny +
                " act=" + READ.getId() +
                " requestContext=" + RESOURCE_FELLES_PERSON +
                " sourceServiceName=" + VEILARB_DOMAIN +
                " cs3Label=deny_cause" +
                " end=" + TIME +
                " flexString1Label=Decision" +
                " flexString2=deny_policy";
    }

    private String expectCefLogAttributesResourcePermit(String resource) {
        return "sproc=" + CALL_ID +
                " flexString1=" + Permit +
                " request=" + REQUEST_PATH +
                " requestContext=" + resource +
                " sourceServiceName=" + VEILARB_DOMAIN +
                " requestMethod=" + REQUEST_METHOD +
                " end=" + TIME +
                " flexString1Label=Decision" +
                " suid=" + TEST_VEILEDER_IDENT +
                " dproc=" + CONSUMER_ID;
    }

    private String expectCefLogAttributesResourceDeny(String resource) {
        return "sproc=" + CALL_ID +
                " flexString2Label=deny_policy" +
                " request=" + REQUEST_PATH +
                " cs5Label=deny_rule" +
                " cs3=cause" +
                " cs5=deny_rule" +
                " requestMethod=" + REQUEST_METHOD +
                " suid=" + TEST_VEILEDER_IDENT +
                " dproc=" + CONSUMER_ID +
                " flexString1=" + Deny +
                " requestContext=" + resource +
                " sourceServiceName=" + VEILARB_DOMAIN +
                " cs3Label=deny_cause" +
                " end=" + TIME +
                " flexString1Label=Decision" +
                " flexString2=deny_policy";
    }

    private String expectCefLogAttributesModiaResourcePermit(String resource) {
        return "sproc=" + CALL_ID +
                " flexString1=" + Permit +
                " request=" + REQUEST_PATH +
                " requestContext=" + resource +
                " sourceServiceName=" + MODIA_DOMAIN +
                " requestMethod=" + REQUEST_METHOD +
                " end=" + TIME +
                " flexString1Label=Decision" +
                " suid=" + TEST_VEILEDER_IDENT +
                " dproc=" + CONSUMER_ID;
    }

    private String expectCefLogAttributesModiaResourceDeny(String resource) {
        return "sproc=" + CALL_ID +
                " flexString2Label=deny_policy" +
                " request=" + REQUEST_PATH +
                " cs5Label=deny_rule" +
                " cs3=cause" +
                " cs5=deny_rule" +
                " requestMethod=" + REQUEST_METHOD +
                " suid=" + TEST_VEILEDER_IDENT +
                " dproc=" + CONSUMER_ID +
                " flexString1=" + Deny +
                " requestContext=" + resource +
                " sourceServiceName=" + MODIA_DOMAIN +
                " cs3Label=deny_cause" +
                " end=" + TIME +
                " flexString1Label=Decision" +
                " flexString2=deny_policy";
    }
}
