package no.nav.apiapp.logging;

import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.MDC;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.core.HttpHeaders;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import static javax.servlet.http.HttpServletResponse.SC_INTERNAL_SERVER_ERROR;
import static javax.ws.rs.core.HttpHeaders.SET_COOKIE;
import static no.nav.apiapp.logging.LogFilter.CALL_ID_HEADER_NAME;
import static no.nav.sbl.rest.RestUtils.CORRELATION_ID_HEADER_NAME;
import static org.assertj.core.api.Assertions.assertThat;
import static org.slf4j.LoggerFactory.getLogger;

public class LogFilterTest {

    private static final Logger LOG = getLogger(LogFilterTest.class);

    private HttpServletRequest httpServletRequest = new MockHttpServletRequest();
    private HttpServletResponse httpServletResponse = new MockHttpServletResponse();

    private LogFilter logFilter = new LogFilter();

    @Test
    public void smoketest() throws ServletException, IOException {
        logFilter.doFilter(httpServletRequest, httpServletResponse, (request, response) -> LOG.info("testing logging 1"));
        logFilter.doFilter(httpServletRequest, httpServletResponse, (request, response) -> LOG.info("testing logging 2"));
        logFilter.doFilter(httpServletRequest, httpServletResponse, (request, response) -> LOG.info("testing logging 3"));
    }

    @Test
    public void asdf() throws ServletException, IOException {
        Map<String, String> initialContextMap = Optional.ofNullable(MDC.getCopyOfContextMap()).orElseGet(HashMap::new);
        logFilter.doFilter(httpServletRequest, httpServletResponse, (request, response) -> {});
        assertThat(initialContextMap).isEqualTo(MDC.getCopyOfContextMap());
    }

    @Test
    public void addResponseHeaders() throws ServletException, IOException {
        logFilter.doFilter(httpServletRequest, httpServletResponse, (request, response) -> {});

        assertThat(httpServletResponse.getHeader(CALL_ID_HEADER_NAME)).isNotEmpty();
        assertThat(httpServletResponse.getHeader(CORRELATION_ID_HEADER_NAME)).isNotEmpty();
        assertThat(httpServletResponse.getHeader(SET_COOKIE)).isNotEmpty();
    }

    @Test
    public void handleExceptions() throws ServletException, IOException {
        logFilter.doFilter(httpServletRequest, httpServletResponse, (request, response) -> fail());
        assertThat(httpServletResponse.getStatus()).isEqualTo(SC_INTERNAL_SERVER_ERROR);
    }

    private void fail() {
        throw new IllegalStateException();
    }

}