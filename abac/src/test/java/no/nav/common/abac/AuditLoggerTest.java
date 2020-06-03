package no.nav.common.abac;

import no.nav.common.abac.cef.CefAbacEventContext;
import no.nav.common.abac.cef.CefAbacResponseMapper;
import no.nav.common.abac.cef.CefEvent.Severity;
import no.nav.common.abac.domain.response.XacmlResponse;
import org.junit.Test;
import org.slf4j.Logger;

import java.util.HashMap;

import static java.lang.String.format;
import static java.util.stream.Collectors.toList;
import static no.nav.common.abac.TestUtils.getContentFromJsonFile;
import static no.nav.common.abac.cef.CefEvent.Severity.INFO;
import static no.nav.common.abac.cef.CefEvent.Severity.WARN;
import static no.nav.common.abac.domain.response.Decision.Deny;
import static no.nav.common.abac.domain.response.Decision.Permit;
import static no.nav.common.utils.IdUtils.generateId;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

public class AuditLoggerTest {

    private static final String APPLICATION_NAME = "ApplicationName";
    private static final String CALL_ID = generateId();
    private static final String CONSUMER_ID = "ConsumingApplication";
    private static final String REQUEST_METHOD = "GET";
    private static final String REQUEST_PATH = "/some/path";
    private static final String SUBJECT_ID = "ABC123";

    private static final long TIME = System.currentTimeMillis();
    private final Logger log = mock(Logger.class);
    private final AuditLogger auditLogger = new AuditLogger(log, () -> TIME);

    @Test
    public void logger_ett_innslag_per_respons() {
        XacmlResponse xacmlResponse = XacmlMapper.mapRawResponse(getContentFromJsonFile("xacmlresponse-multiple-decision-and-category.json"));
        CefAbacResponseMapper mapper = new CefAbacResponseMapper(
                params ->
                        xacmlResponse.getResponse().stream()
                                .map(response -> {
                                    HashMap<String, String> attributes = new HashMap<>();
                                    attributes.put("duid", response.getCategory().get(0).getAttribute().getValue());
                                    return new CefAbacResponseMapper.Result(response, attributes);
                                })
                                .collect(toList())
        );

        CefAbacEventContext context = CefAbacEventContext.builder()
                .applicationName(APPLICATION_NAME)
                .callId(CALL_ID)
                .consumerId(CONSUMER_ID)
                .requestMethod(REQUEST_METHOD)
                .requestPath(REQUEST_PATH)
                .mapper(mapper)
                .subjectId(SUBJECT_ID)
                .build();

        auditLogger.logCef(null, xacmlResponse, context);

        verify(log).info(eq(expectHeader(INFO) + expectAttributesFlerePermit()));
        verify(log).info(eq(expectHeader(WARN) + expectAttributesFlereDeny()));
    }

    private String expectHeader(Severity severity) {
        return format("CEF:0|%s|Sporingslogg|1.0|audit:access|ABAC Sporingslogg|%s|", APPLICATION_NAME, severity);
    }

    private String expectAttributesFlerePermit() {
        return "sproc=" + CALL_ID +
                " flexString1=" + Permit +
                " request=" + REQUEST_PATH +
                " duid=11111111111" +
                " requestMethod=" + REQUEST_METHOD +
                " end=" + TIME +
                " flexString1Label=Decision" +
                " suid=" + SUBJECT_ID +
                " dproc=" + CONSUMER_ID;
    }

    private String expectAttributesFlereDeny() {
        return "sproc=" + CALL_ID +
                " flexString2Label=deny_policy" +
                " request=" + REQUEST_PATH +
                " cs5Label=deny_rule" +
                " cs3=cause" +
                " duid=22222222222" +
                " cs5=deny_rule" +
                " requestMethod=" + REQUEST_METHOD +
                " suid=" + SUBJECT_ID +
                " dproc=" + CONSUMER_ID +
                " flexString1=" + Deny +
                " cs3Label=deny_cause" +
                " end=" + TIME +
                " flexString1Label=Decision" +
                " flexString2=deny_policy";
    }
}
