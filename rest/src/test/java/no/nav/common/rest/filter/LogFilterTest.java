package no.nav.common.rest.filter;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.slf4j.Logger;
import org.slf4j.MDC;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static no.nav.common.rest.filter.LogRequestFilter.NAV_CALL_ID_HEADER_NAME;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;
import static org.slf4j.LoggerFactory.getLogger;

public class LogFilterTest {

    private static final Logger LOG = getLogger(LogFilterTest.class);

    private HttpServletRequest httpServletRequest = mock(HttpServletRequest.class);
    private HttpServletResponse httpServletResponse = mock(HttpServletResponse.class);

    private LogRequestFilter logRequestFilter = new LogRequestFilter("test");

    @Before
    public void setup() {
        when(httpServletRequest.getMethod()).thenReturn("GET");
        when(httpServletRequest.getRequestURI()).thenReturn("/test/path");
    }

    @Test
    public void smoketest() throws ServletException, IOException {
        logRequestFilter.doFilter(httpServletRequest, httpServletResponse, (request, response) -> LOG.info("testing logging 1"));
        logRequestFilter.doFilter(httpServletRequest, httpServletResponse, (request, response) -> LOG.info("testing logging 2"));
        logRequestFilter.doFilter(httpServletRequest, httpServletResponse, (request, response) -> LOG.info("testing logging 3"));
    }

    @Test
    public void cleanupOfMDCContext() throws ServletException, IOException {
        Map<String, String> initialContextMap = Optional.ofNullable(MDC.getCopyOfContextMap()).orElseGet(HashMap::new);
        logRequestFilter.doFilter(httpServletRequest, httpServletResponse, (request, response) -> {});
        assertThat(initialContextMap).isEqualTo(MDC.getCopyOfContextMap());
    }

    @Test
    public void addResponseHeaders() throws ServletException, IOException {
        logRequestFilter.doFilter(httpServletRequest, httpServletResponse, (request, response) -> {});

        ArgumentCaptor<String> captor = ArgumentCaptor.forClass(String.class);

        verify(httpServletResponse, times(1)).setHeader(eq(NAV_CALL_ID_HEADER_NAME), captor.capture());
        verify(httpServletResponse, times(1)).setHeader(eq("Server"), captor.capture());
        verify(httpServletResponse, times(1)).addCookie(any());

        List<String> values = captor.getAllValues();

        assertThat(values.get(0)).matches("[a-z0-9]{28,}");
        assertThat(values.get(1)).isEqualTo("test");
    }

    @Test
    public void handleExceptions() throws ServletException, IOException {
        logRequestFilter.doFilter(httpServletRequest, httpServletResponse, (request, response) -> fail());
        verify(httpServletResponse, times(1)).setStatus(500);
    }

    private void fail() {
        throw new IllegalStateException();
    }

}