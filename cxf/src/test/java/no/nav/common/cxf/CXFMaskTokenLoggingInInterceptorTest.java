package no.nav.common.cxf;

import org.apache.cxf.ext.logging.event.LogEvent;
import org.junit.Test;

import java.util.LinkedHashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

public class CXFMaskTokenLoggingInInterceptorTest {

    @Test
    public void skalFjerneCookieFraHttpHeaders() {
        CXFMaskTokenLoggingInInterceptor interceptor = new CXFMaskTokenLoggingInInterceptor();

        LogEvent event = new LogEvent();
        Map<String, String> headers = new LinkedHashMap<>();
        headers.put("Cookie", "ID_Token=ekjfbsd; refresh_token=ekjfdbsd");
        headers.put("Accept", "text/html");
        event.setHeaders(headers);

        interceptor.maskIfEnabled(event);

        assertThat(event.getHeaders()).doesNotContainKey("Cookie");
        assertThat(event.getHeaders()).containsKey("Accept");
    }

    @Test
    public void skalIkkeMaskereNaarDeaktivert() {
        CXFMaskTokenLoggingInInterceptor interceptor = new CXFMaskTokenLoggingInInterceptor();
        interceptor.setMaskerTokenLogging(false);

        LogEvent event = new LogEvent();
        Map<String, String> headers = new LinkedHashMap<>();
        headers.put("Cookie", "ID_Token=ekjfbsd");
        event.setHeaders(headers);

        interceptor.maskIfEnabled(event);

        assertThat(event.getHeaders()).containsKey("Cookie");
    }
}