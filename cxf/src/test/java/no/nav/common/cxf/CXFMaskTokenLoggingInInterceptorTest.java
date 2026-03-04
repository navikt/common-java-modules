package no.nav.common.cxf;

import org.apache.cxf.ext.logging.event.LogEvent;
import org.junit.Test;

import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

public class CXFMaskTokenLoggingInInterceptorTest {

    @Test
    public void skalFjerneCookieFraHttpHeaders() {
        CXFMaskTokenLoggingInInterceptor interceptor = new CXFMaskTokenLoggingInInterceptor();

        LogEvent event = new LogEvent();
        Map<String, String> headers = new HashMap<>();
        headers.put("Cookie", "ID_Token=ekjfbsd; refresh_token=ekjfdbsd");
        headers.put("Accept", "text/html");
        event.setHeaders(headers);

        assertThat(event.getHeaders()).containsKey("Cookie");
        interceptor.mask(event);
        assertThat(event.getHeaders()).doesNotContainKey("Cookie");
    }

}