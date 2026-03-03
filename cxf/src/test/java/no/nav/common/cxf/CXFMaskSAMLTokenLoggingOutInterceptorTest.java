package no.nav.common.cxf;

import org.apache.cxf.ext.logging.event.LogEvent;
import org.junit.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.StringContains.containsString;
import static org.hamcrest.core.IsNot.not;

public class CXFMaskSAMLTokenLoggingOutInterceptorTest {
    CXFMaskSAMLTokenLoggingOutInterceptor interceptor = new CXFMaskSAMLTokenLoggingOutInterceptor();

    @Test
    public void skalFjerneSAMLTokenFraXmlPayload() {
        LogEvent event = new LogEvent();
        event.setContentType("text/xml");
        event.setPayload(
            "<soap:Envelope xmlns:soap=\"http://schemas.xmlsoap.org/soap/envelope/\">" +
            "<soap:Header><wsse:Security xmlns:wsse=\"http://docs.oasis-open.org/wss/2004/01/oasis-200401-wss-wssecurity-secext-1.0.xsd\">secret-token</wsse:Security></soap:Header>" +
            "<soap:Body><request>data</request></soap:Body>" +
            "</soap:Envelope>"
        );

        interceptor.send(event);

        assertThat(event.getPayload(), not(containsString("secret-token")));
        assertThat(event.getPayload(), containsString("data"));
    }

    @Test
    public void skalIkkeFjerneSAMLTokenNaarMaskeringErAvslaatt() {
        interceptor.setMaskerSAMLToken(false);
        LogEvent event = new LogEvent();
        event.setContentType("text/xml");
        event.setPayload(
            "<soap:Envelope xmlns:soap=\"http://schemas.xmlsoap.org/soap/envelope/\">" +
            "<soap:Header><wsse:Security>secret-token</wsse:Security></soap:Header>" +
            "</soap:Envelope>"
        );

        interceptor.send(event);

        assertThat(event.getPayload(), containsString("secret-token"));
    }
}