package no.nav.common.cxf;

import org.apache.cxf.ext.logging.event.LogEvent;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class CXFMaskSAMLTokenLoggingOutInterceptorTest {

    private static final String XML_MED_SAML =
            "<soap:Envelope xmlns:soap=\"http://schemas.xmlsoap.org/soap/envelope/\">"
                    + "<soap:Header>"
                    + "<wsse:Security xmlns:wsse=\"wsse-ns\"><Assertion>hemmelig</Assertion></wsse:Security>"
                    + "</soap:Header>"
                    + "<soap:Body><ping/></soap:Body>"
                    + "</soap:Envelope>";

    @Test
    public void skalFjerneSecurityHeaderFraXmlPayload() {
        CXFMaskSAMLTokenLoggingOutInterceptor interceptor = new CXFMaskSAMLTokenLoggingOutInterceptor();

        LogEvent event = new LogEvent();
        event.setContentType("application/xml");
        event.setPayload(XML_MED_SAML);

        interceptor.maskIfEnabled(event);

        assertThat(event.getPayload()).doesNotContain("hemmelig");
        assertThat(event.getPayload()).doesNotContain("Security");
        assertThat(event.getPayload()).contains("<ping");
    }

    @Test
    public void skalIkkeMaskereNaarDeaktivert() {
        CXFMaskSAMLTokenLoggingOutInterceptor interceptor = new CXFMaskSAMLTokenLoggingOutInterceptor();
        interceptor.setMaskerSAMLToken(false);

        LogEvent event = new LogEvent();
        event.setContentType("application/xml");
        event.setPayload(XML_MED_SAML);

        interceptor.maskIfEnabled(event);

        assertThat(event.getPayload()).contains("hemmelig");
    }

    @Test
    public void skalIkkeMaskereIkkeXmlPayload() {
        CXFMaskSAMLTokenLoggingOutInterceptor interceptor = new CXFMaskSAMLTokenLoggingOutInterceptor();

        LogEvent event = new LogEvent();
        event.setContentType("application/json");
        event.setPayload("{\"Security\":\"hemmelig\"}");

        interceptor.maskIfEnabled(event);

        assertThat(event.getPayload()).contains("hemmelig");
    }
}