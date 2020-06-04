package no.nav.common.abac;

import no.nav.common.abac.cef.CefEvent;
import no.nav.common.abac.domain.AbacPersonId;
import no.nav.common.abac.domain.request.XacmlRequest;
import no.nav.common.abac.domain.response.XacmlResponse;
import no.nav.common.abac.exception.PepException;
import no.nav.common.test.junit.SystemPropertiesRule;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.slf4j.Logger;

import static java.lang.String.format;
import static no.nav.common.abac.NavAttributter.*;
import static no.nav.common.abac.TestUtils.assertJsonEquals;
import static no.nav.common.abac.TestUtils.getContentFromJsonFile;
import static no.nav.common.abac.VeilarbPep.VEILARB_DOMAIN;
import static no.nav.common.abac.cef.CefEvent.Severity.INFO;
import static no.nav.common.abac.cef.CefEvent.Severity.WARN;
import static no.nav.common.abac.domain.request.ActionId.READ;
import static no.nav.common.abac.domain.response.Decision.Deny;
import static no.nav.common.abac.domain.response.Decision.Permit;
import static no.nav.common.utils.EnvironmentUtils.NAIS_APP_NAME_PROPERTY_NAME;
import static no.nav.common.utils.IdUtils.generateId;
import static org.mockito.Mockito.*;

public class VeilarbPepTest {

    private final static AbacPersonId TEST_FNR = AbacPersonId.fnr("12345678900");

    private final static String TEST_SRV_USERNAME = "test";
    private final static String TEST_VEILEDER_IDENT = "Z1234";
    private final static String TEST_ENHET_ID = "1234";
    private static final String TEST_OIDC_TOKEN_BODY = "eyJpc3MiOiJuYXYubm8iLCJleHAiOjE0ODQ2NTI2NzIsImp0aSI6IkZHdXJVYWdleFRwTUVZTjdMRHlsQ1EiLCJpYXQiOjE0ODQ2NTIwNzIsIm5iZiI6MTQ4NDY1MTk1Miwic3ViIjoiYTExMTExMSJ9";
    private final static String APPLICATION_NAME = "testapp";
    private static final long TIME = System.currentTimeMillis();
    private static final String CALL_ID = generateId();
    private static final String CONSUMER_ID = "ConsumingApplication";
    private static final String REQUEST_METHOD = "GET";
    private static final String REQUEST_PATH = "/some/path";
    private static final String ENHET = "enhet321";
    private final static AuditRequestInfo AUDIT_REQUEST_INFO = new AuditRequestInfo(CALL_ID, CONSUMER_ID, REQUEST_METHOD, REQUEST_PATH);

    private final Logger log = mock(Logger.class);
    private final SubjectProvider subjectProvider = mock(SubjectProvider.class);
    private final AuditLogger auditLogger = new AuditLogger(log, () -> TIME);
    private final AuditRequestInfoSupplier auditRequestInfoSupplier = () -> AUDIT_REQUEST_INFO;

    @Rule
    public SystemPropertiesRule systemPropertiesRule = new SystemPropertiesRule();

    @Before
    public void setup() {
        systemPropertiesRule.setProperty(NAIS_APP_NAME_PROPERTY_NAME, APPLICATION_NAME);
        when(subjectProvider.getSubjectFromToken(TEST_OIDC_TOKEN_BODY)).thenReturn(TEST_VEILEDER_IDENT);
    }

    private final AbacClient genericPermitClient = abacClientSpyWithResponseFromFile("xacmlresponse-generic-permit.json");
    private final AbacClient genericDenyClient = abacClientSpyWithResponseFromFile("xacmlresponse-generic-deny.json");

    @Test
    public void sjekkVeiledertilgangTilEnhet__skal_lage_riktig_request() {
        VeilarbPep veilarbPep = new VeilarbPep(TEST_SRV_USERNAME, genericPermitClient, auditLogger, subjectProvider, auditRequestInfoSupplier);
        String expectedRequest = getContentFromJsonFile("xacmlrequest-sjekkVeiledertilgangTilEnhet.json");
        ArgumentCaptor<String> captor = ArgumentCaptor.forClass(String.class);

        veilarbPep.sjekkVeiledertilgangTilEnhet(TEST_VEILEDER_IDENT, TEST_ENHET_ID);

        verify(genericPermitClient, times(1)).sendRawRequest(captor.capture());
        assertJsonEquals(expectedRequest, captor.getValue());
    }

    @Test(expected = PepException.class)
    public void sjekkVeiledertilgangTilEnhet__skal_kaste_exception_hvis_ikke_tilgang() {
        VeilarbPep veilarbPep = new VeilarbPep(TEST_SRV_USERNAME, genericDenyClient, auditLogger, subjectProvider, auditRequestInfoSupplier);
        veilarbPep.sjekkVeiledertilgangTilEnhet(TEST_VEILEDER_IDENT, TEST_ENHET_ID);
    }


    @Test
    public void sjekkVeiledertilgangTilPerson__skal_lage_riktig_request() {
        VeilarbPep veilarbPep = new VeilarbPep(TEST_SRV_USERNAME, genericPermitClient, auditLogger, subjectProvider, auditRequestInfoSupplier);
        String expectedRequest = getContentFromJsonFile("xacmlrequest-sjekkVeiledertilgangTilPerson.json");
        ArgumentCaptor<String> captor = ArgumentCaptor.forClass(String.class);

        veilarbPep.sjekkVeiledertilgangTilPerson(TEST_VEILEDER_IDENT, READ, TEST_FNR);

        verify(genericPermitClient, times(1)).sendRawRequest(captor.capture());
        assertJsonEquals(expectedRequest, captor.getValue());
    }

    @Test(expected = PepException.class)
    public void sjekkVeiledertilgangTilPerson__skal_kaste_exception_hvis_ikke_tilgang() {
        VeilarbPep veilarbPep = new VeilarbPep(TEST_SRV_USERNAME, genericDenyClient, auditLogger, subjectProvider, auditRequestInfoSupplier);
        veilarbPep.sjekkVeiledertilgangTilPerson(TEST_VEILEDER_IDENT, READ, TEST_FNR);
    }


    @Test
    public void sjekkTilgangTilPerson__skal_lage_riktig_request() {
        VeilarbPep veilarbPep = new VeilarbPep(TEST_SRV_USERNAME, genericPermitClient, auditLogger, subjectProvider, auditRequestInfoSupplier);
        String expectedRequest = getContentFromJsonFile("xacmlrequest-sjekkTilgangTilPerson.json");
        ArgumentCaptor<String> captor = ArgumentCaptor.forClass(String.class);

        veilarbPep.sjekkTilgangTilPerson(TEST_OIDC_TOKEN_BODY, READ, TEST_FNR);

        verify(genericPermitClient, times(1)).sendRawRequest(captor.capture());
        assertJsonEquals(expectedRequest, captor.getValue());
    }

    @Test(expected = PepException.class)
    public void sjekkTilgangTilPerson__skal_kaste_exception_hvis_ikke_tilgang() {
        VeilarbPep veilarbPep = new VeilarbPep(TEST_SRV_USERNAME, genericDenyClient, auditLogger, subjectProvider, auditRequestInfoSupplier);
        veilarbPep.sjekkTilgangTilPerson(TEST_OIDC_TOKEN_BODY, READ, TEST_FNR);
    }


    @Test
    public void sjekkVeiledertilgangTilKode6__skal_lage_riktig_request() {
        VeilarbPep veilarbPep = new VeilarbPep(TEST_SRV_USERNAME, genericPermitClient, auditLogger, subjectProvider, auditRequestInfoSupplier);
        String expectedRequest = getContentFromJsonFile("xacmlrequest-sjekkVeiledertilgangTilKode6.json");
        ArgumentCaptor<String> captor = ArgumentCaptor.forClass(String.class);

        veilarbPep.sjekkVeiledertilgangTilKode6(TEST_VEILEDER_IDENT);

        verify(genericPermitClient, times(1)).sendRawRequest(captor.capture());
        assertJsonEquals(expectedRequest, captor.getValue());
    }

    @Test(expected = PepException.class)
    public void sjekkVeiledertilgangTilKode6__skal_kaste_exception_hvis_ikke_tilgang() {
        VeilarbPep veilarbPep = new VeilarbPep(TEST_SRV_USERNAME, genericDenyClient, auditLogger, subjectProvider, auditRequestInfoSupplier);
        veilarbPep.sjekkVeiledertilgangTilKode6(TEST_VEILEDER_IDENT);
    }

    @Test
    public void sjekkVeiledertilgangTilKode7__skal_lage_riktig_request() {
        VeilarbPep veilarbPep = new VeilarbPep(TEST_SRV_USERNAME, genericPermitClient, auditLogger, subjectProvider, auditRequestInfoSupplier);
        String expectedRequest = getContentFromJsonFile("xacmlrequest-sjekkVeiledertilgangTilKode7.json");
        ArgumentCaptor<String> captor = ArgumentCaptor.forClass(String.class);

        veilarbPep.sjekkVeiledertilgangTilKode7(TEST_VEILEDER_IDENT);

        verify(genericPermitClient, times(1)).sendRawRequest(captor.capture());
        assertJsonEquals(expectedRequest, captor.getValue());
    }

    @Test(expected = PepException.class)
    public void sjekkVeiledertilgangTilKode7__skal_kaste_exception_hvis_ikke_tilgang() {
        VeilarbPep veilarbPep = new VeilarbPep(TEST_SRV_USERNAME, genericDenyClient, auditLogger, subjectProvider, auditRequestInfoSupplier);
        veilarbPep.sjekkVeiledertilgangTilKode7(TEST_VEILEDER_IDENT);
    }

    @Test
    public void sjekkVeiledertilgangTilEgenAnsatt__skal_lage_riktig_request() {
        VeilarbPep veilarbPep = new VeilarbPep(TEST_SRV_USERNAME, genericPermitClient, auditLogger, subjectProvider, auditRequestInfoSupplier);
        String expectedRequest = getContentFromJsonFile("xacmlrequest-sjekkVeiledertilgangTilEgenAnsatt.json");
        ArgumentCaptor<String> captor = ArgumentCaptor.forClass(String.class);

        veilarbPep.sjekkVeiledertilgangTilEgenAnsatt(TEST_VEILEDER_IDENT);

        verify(genericPermitClient, times(1)).sendRawRequest(captor.capture());
        assertJsonEquals(expectedRequest, captor.getValue());
    }

    @Test(expected = PepException.class)
    public void sjekkVeiledertilgangTilEgenAnsatt__skal_kaste_exception_hvis_ikke_tilgang() {
        VeilarbPep veilarbPep = new VeilarbPep(TEST_SRV_USERNAME, genericDenyClient, auditLogger, subjectProvider, auditRequestInfoSupplier);
        veilarbPep.sjekkVeiledertilgangTilEgenAnsatt(TEST_VEILEDER_IDENT);
    }

    @Test
    public void sjekkVeiledertilgangTilEnhet__riktig_audit_log_for_permit() {
        VeilarbPep veilarbPep = new VeilarbPep(TEST_SRV_USERNAME, genericPermitClient, auditLogger, subjectProvider, auditRequestInfoSupplier);
        veilarbPep.sjekkVeiledertilgangTilEnhet(TEST_VEILEDER_IDENT, ENHET);
        verify(log).info(eq(expectCefLogHeader(INFO) + expectCefLogAttributesEnhetPermit()));
    }

    @Test
    public void sjekkVeiledertilgangTilEnhet__riktig_audit_log_for_deny() {
        VeilarbPep veilarbPep = new VeilarbPep(TEST_SRV_USERNAME, genericDenyClient, auditLogger, subjectProvider, auditRequestInfoSupplier);
        try {
            veilarbPep.sjekkVeiledertilgangTilEnhet(TEST_VEILEDER_IDENT, ENHET);
        } catch (PepException ignored) {
        }
        verify(log).info(eq(expectCefLogHeader(WARN) + expectCefLogAttributesEnhetDeny()));
    }

    @Test
    public void sjekkVeiledertilgangTilPerson__riktig_audit_log_for_permit() {
        VeilarbPep veilarbPep = new VeilarbPep(TEST_SRV_USERNAME, genericPermitClient, auditLogger, subjectProvider, auditRequestInfoSupplier);
        veilarbPep.sjekkVeiledertilgangTilPerson(TEST_VEILEDER_IDENT, READ, TEST_FNR);
        verify(log).info(eq(expectCefLogHeader(INFO) + expectCefLogAttributesPersonPermit()));
    }

    @Test
    public void sjekkVeiledertilgangTilPerson__riktig_audit_log_for_deny() {
        VeilarbPep veilarbPep = new VeilarbPep(TEST_SRV_USERNAME, genericDenyClient, auditLogger, subjectProvider, auditRequestInfoSupplier);
        try {
            veilarbPep.sjekkVeiledertilgangTilPerson(TEST_VEILEDER_IDENT, READ, TEST_FNR);
        } catch (PepException ignored) {
        }
        verify(log).info(eq(expectCefLogHeader(WARN) + expectCefLogAttributesPersonDeny()));
    }

    @Test
    public void sjekkTilgangTilPerson__riktig_audit_log_for_permit() {
        VeilarbPep veilarbPep = new VeilarbPep(TEST_SRV_USERNAME, genericPermitClient, auditLogger, subjectProvider, auditRequestInfoSupplier);
        veilarbPep.sjekkTilgangTilPerson(TEST_OIDC_TOKEN_BODY, READ, TEST_FNR);
        verify(log).info(eq(expectCefLogHeader(INFO) + expectCefLogAttributesPersonPermit()));
    }

    @Test
    public void sjekkTilgangTilPerson__riktig_audit_log_for_deny() {
        VeilarbPep veilarbPep = new VeilarbPep(TEST_SRV_USERNAME, genericDenyClient, auditLogger, subjectProvider, auditRequestInfoSupplier);
        try {
            veilarbPep.sjekkTilgangTilPerson(TEST_OIDC_TOKEN_BODY, READ, TEST_FNR);
        } catch (PepException ignored) {
        }
        verify(log).info(eq(expectCefLogHeader(WARN) + expectCefLogAttributesPersonDeny()));
    }

    @Test
    public void sjekkVeiledertilgangTilKode6__riktig_audit_log_for_permit() {
        VeilarbPep veilarbPep = new VeilarbPep(TEST_SRV_USERNAME, genericPermitClient, auditLogger, subjectProvider, auditRequestInfoSupplier);
        veilarbPep.sjekkVeiledertilgangTilKode6(TEST_VEILEDER_IDENT);
        verify(log).info(eq(expectCefLogHeader(INFO) + expectCefLogAttributesResourcePermit(SUBJECT_FELLES_HAR_TILGANG_KODE_6)));
    }

    @Test
    public void sjekkVeiledertilgangTilKode6__riktig_audit_log_for_deny() {
        VeilarbPep veilarbPep = new VeilarbPep(TEST_SRV_USERNAME, genericDenyClient, auditLogger, subjectProvider, auditRequestInfoSupplier);
        try {
            veilarbPep.sjekkVeiledertilgangTilKode6(TEST_VEILEDER_IDENT);
        } catch (PepException ignored) {
        }
        verify(log).info(eq(expectCefLogHeader(WARN) + expectCefLogAttributesResourceDeny(SUBJECT_FELLES_HAR_TILGANG_KODE_6)));
    }

    @Test
    public void sjekkVeiledertilgangTilKode7__riktig_audit_log_for_permit() {
        VeilarbPep veilarbPep = new VeilarbPep(TEST_SRV_USERNAME, genericPermitClient, auditLogger, subjectProvider, auditRequestInfoSupplier);
        veilarbPep.sjekkVeiledertilgangTilKode7(TEST_VEILEDER_IDENT);
        verify(log).info(eq(expectCefLogHeader(INFO) + expectCefLogAttributesResourcePermit(SUBJECT_FELLES_HAR_TILGANG_KODE_7)));
    }

    @Test
    public void sjekkVeiledertilgangTilKode7__riktig_audit_log_for_deny() {
        VeilarbPep veilarbPep = new VeilarbPep(TEST_SRV_USERNAME, genericDenyClient, auditLogger, subjectProvider, auditRequestInfoSupplier);
        try {
            veilarbPep.sjekkVeiledertilgangTilKode7(TEST_VEILEDER_IDENT);
        } catch (PepException ignored) {
        }
        verify(log).info(eq(expectCefLogHeader(WARN) + expectCefLogAttributesResourceDeny(SUBJECT_FELLES_HAR_TILGANG_KODE_7)));
    }

    @Test
    public void sjekkVeiledertilgangTilEgenAnsatt__riktig_audit_log_for_permit() {
        VeilarbPep veilarbPep = new VeilarbPep(TEST_SRV_USERNAME, genericPermitClient, auditLogger, subjectProvider, auditRequestInfoSupplier);
        veilarbPep.sjekkVeiledertilgangTilEgenAnsatt(TEST_VEILEDER_IDENT);
        verify(log).info(eq(expectCefLogHeader(INFO) + expectCefLogAttributesResourcePermit(SUBJECT_FELLES_HAR_TILGANG_EGEN_ANSATT)));
    }

    @Test
    public void sjekkVeiledertilgangTilEgenAnsatt__riktig_audit_log_for_deny() {
        VeilarbPep veilarbPep = new VeilarbPep(TEST_SRV_USERNAME, genericDenyClient, auditLogger, subjectProvider, auditRequestInfoSupplier);
        try {
            veilarbPep.sjekkVeiledertilgangTilEgenAnsatt(TEST_VEILEDER_IDENT);
        } catch (PepException ignored) {
        }
        verify(log).info(eq(expectCefLogHeader(WARN) + expectCefLogAttributesResourceDeny(SUBJECT_FELLES_HAR_TILGANG_EGEN_ANSATT)));
    }

    private AbacClient abacClientSpyWithResponseFromFile(String fileName) {
        return spy(new AbacClient() {
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
                " requestContext=" + RESOURCE_VEILARB_ENHET_EIENDEL +
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
                " requestContext=" + RESOURCE_VEILARB_ENHET_EIENDEL +
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
                " duid=" + TEST_FNR.getId() +
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
                " duid=" + TEST_FNR.getId() +
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
}
