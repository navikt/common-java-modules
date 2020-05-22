package no.nav.sbl.dialogarena.common.abac.pep;

import com.github.tomakehurst.wiremock.junit.WireMockRule;
import lombok.SneakyThrows;
import no.nav.log.cef.CefEvent.Severity;
import no.nav.sbl.dialogarena.common.abac.pep.cef.CefEventContext;
import no.nav.sbl.dialogarena.common.abac.pep.cef.CefEventResource;
import no.nav.sbl.dialogarena.common.abac.pep.domain.ResourceType;
import no.nav.sbl.dialogarena.common.abac.pep.domain.request.XacmlRequest;
import no.nav.sbl.dialogarena.common.abac.pep.domain.response.Decision;
import no.nav.sbl.dialogarena.common.abac.pep.domain.response.Response;
import no.nav.sbl.dialogarena.common.abac.pep.domain.response.XacmlResponse;
import no.nav.sbl.dialogarena.common.abac.pep.service.AbacService;
import no.nav.sbl.dialogarena.common.abac.pep.service.AbacServiceConfig;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.slf4j.Logger;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static java.lang.String.format;
import static java.util.stream.Collectors.toMap;
import static no.nav.common.utils.IdUtils.generateId;
import static no.nav.log.cef.CefEvent.Severity.INFO;
import static no.nav.log.cef.CefEvent.Severity.WARN;
import static no.nav.sbl.dialogarena.common.abac.TestUtils.getContentFromJsonFile;
import static no.nav.sbl.dialogarena.common.abac.pep.domain.response.Decision.Deny;
import static no.nav.sbl.dialogarena.common.abac.pep.domain.response.Decision.Permit;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

public class CefAbacLoggingTest {

    private PepImpl pep;
    private AuditLogger auditLogger;
    private AbacService abacService;
    private final Logger log = mock(Logger.class);

    private static final String APPLICATION_NAME = "ApplicationName";
    private static final String CALL_ID = generateId();
    private static final String REQUEST_METHOD = "GET";
    private static final String REQUEST_PATH = "/some/path";
    private static final String SUBJECT_ID = "ABC123";
    private static final long TIME = System.currentTimeMillis();
    private static final AbacPersonId PERSON_ID = AbacPersonId.aktorId("123");
    private static final String ENHET = "enhet321";

    @Rule
    public WireMockRule wireMockRule = new WireMockRule(0);

    @Before
    public void setup() {
        String endpointUrl = "http://localhost:" + wireMockRule.port();

        AbacServiceConfig abacServiceConfig = AbacServiceConfig.builder()
                .username("username")
                .password("password")
                .endpointUrl(endpointUrl)
                .build();

        abacService = new AbacService(abacServiceConfig);
        auditLogger = new AuditLogger(log, () -> TIME);
        pep = new PepImpl(abacService, auditLogger);
    }

    @Test
    public void permit_log_for_person() {

        responseWithJson("xacmlresponse-permit-simple.json");

        RequestData requestData = requestData(ResourceType.VeilArbPerson).withPersonId(PERSON_ID);
        CefEventContext cefEventContext = eventContext(CefEventResource.personId(PERSON_ID));

        pep.harTilgang(requestData, cefEventContext);

        verify(log).info(eq(expectHeader(INFO) + expectAttributesPerson(Permit)));
    }

    @Test
    public void deny_log_for_person() {
        responseWithJson("xacmlresponse-deny.json");

        RequestData requestData = requestData(ResourceType.VeilArbPerson).withPersonId(PERSON_ID);
        CefEventContext cefEventContext = eventContext(CefEventResource.personId(PERSON_ID));

        pep.harTilgang(requestData, cefEventContext);

        verify(log).info(eq(expectHeader(WARN) + expectAttributesPerson(Deny)));
    }

    @Test
    public void permit_log_for_enhet() {
        responseWithJson("xacmlresponse-permit-simple.json");

        RequestData requestData = requestData(ResourceType.Enhet).withEnhet(ENHET);
        CefEventContext cefEventContext = eventContext(CefEventResource.enhetId(ENHET));

        pep.harTilgang(requestData, cefEventContext);

        verify(log).info(eq(expectHeader(INFO) + expectAttributesEnhet(Permit)));
    }

    @Test
    public void deny_log_for_enhet() {
        responseWithJson("xacmlresponse-deny.json");

        RequestData requestData = requestData(ResourceType.Enhet).withEnhet(ENHET);
        CefEventContext cefEventContext = eventContext(CefEventResource.enhetId(ENHET));

        pep.harTilgang(requestData, cefEventContext);

        verify(log).info(eq(expectHeader(WARN) + expectAttributesEnhet(Deny)));
    }

    @Test
    @SneakyThrows
    public void log_for_flere() {
        responseWithJson("xacmlresponse-multiple-decision-and-category.json");

        RequestData requestData = requestData(ResourceType.VeilArbPerson).withPersonId(PERSON_ID);
        CefEventContext cefEventContext = eventContext(CefEventResource.list(xacmlResponse ->
                xacmlResponse.getResponse().stream()
                        .collect(toMap(x -> x.getCategory().get(0).getAttribute().getValue(), Response::getDecision))));

        XacmlResponse xacmlResponse = abacService
                .askForPermission(new XacmlRequest().withRequest(new XacmlRequestGenerator().makeRequest(requestData)));

        auditLogger.logCEF(xacmlResponse, cefEventContext);

        verify(log).info(eq(expectHeader(WARN) + expectAttributesFlere()));
    }

    private void responseWithJson(String jsonFile) {
        givenThat(post("/").willReturn(aResponse()
                .withStatus(200)
                .withBody(getContentFromJsonFile(jsonFile))));
    }

    private String expectHeader(Severity severity) {

        return format("CEF:0|%s|Sporingslogg|1.0|trace:access|ABAC Sporingslogg|%s|", APPLICATION_NAME, severity);
    }

    private String expectAttributesPerson(Decision decision) {
        return format("sproc=%s flexString1=%s request=%s duid=%s requestMethod=%s end=%s flexString1Label=Decision suid=%s",
                CALL_ID, decision, REQUEST_PATH, PERSON_ID.getId(), REQUEST_METHOD, TIME, SUBJECT_ID);
    }
    private String expectAttributesEnhet(Decision decision) {
        return format("sproc=%s flexString1=%s request=%s requestMethod=%s deviceCustomString1=%s end=%s flexString1Label=Decision suid=%s",
                CALL_ID, decision, REQUEST_PATH, REQUEST_METHOD, ENHET, TIME, SUBJECT_ID);
    }

    private String expectAttributesFlere() {
        return format("sproc=%s request=%s cs5Label=Decisions cs3=11111111111 22222222222 cs5=Permit Deny cs3Label=Resources requestMethod=%s end=%s suid=%s",
                CALL_ID, REQUEST_PATH, REQUEST_METHOD, TIME, SUBJECT_ID);
    }


    private RequestData requestData(ResourceType resourceType) {
        return new RequestData()
                .withResourceType(resourceType)
                .withDomain("veilarb")
                .withOidcToken("token")
                .withCredentialResource("credential_resource");
    }

    private CefEventContext eventContext(CefEventResource resource) {
        return CefEventContext.builder()
                .applicationName(APPLICATION_NAME)
                .callId(CALL_ID)
                .requestMethod(REQUEST_METHOD)
                .requestPath(REQUEST_PATH)
                .resource(resource)
                .subjectId(SUBJECT_ID)
                .build();
    }
}
