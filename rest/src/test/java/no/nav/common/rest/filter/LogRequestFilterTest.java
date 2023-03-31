package no.nav.common.rest.filter;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import no.nav.common.log.MDCConstants;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.slf4j.LoggerFactory;

import java.io.IOException;

import static no.nav.common.rest.filter.LogRequestFilter.NAV_CALL_ID_HEADER_NAME;
import static no.nav.common.rest.filter.LogRequestFilter.NAV_CONSUMER_ID_HEADER_NAME;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class LogRequestFilterTest {
    @Mock
    private HttpServletRequest request;

    @Mock
    private HttpServletResponse response;

    @Mock
    private FilterChain filterChain;

    private LogRequestFilter logRequestFilter;

    private ListAppender<ILoggingEvent> filterAppender;

    private final Logger filterLogger = (Logger)LoggerFactory.getLogger(LogRequestFilter.class);

    @Before
    public void setUp() {
        MockitoAnnotations.openMocks(this);

        filterAppender = new ListAppender<>();
        filterAppender.start();
        filterLogger.addAppender(filterAppender);

        logRequestFilter = new LogRequestFilter("veilarboppfolging");
        when(request.getRequestURI()).thenReturn("/api/endpoint1");
        when(request.getRequestURL()).thenReturn(new StringBuffer("/api/endpoint1"));
        when(request.getHeader(NAV_CONSUMER_ID_HEADER_NAME)).thenReturn("callingApp");
        when(request.getHeader(NAV_CALL_ID_HEADER_NAME)).thenReturn("some_call_id");
    }

    @Test
    public void testDoFilter() throws ServletException, IOException {
        logRequestFilter.doFilter(request, response, filterChain);

        verify(filterChain).doFilter(request, response);


        // Add more assertions to test the behavior of the filter
    }

    @Test
    public void testResponseSuccess() throws ServletException, IOException {
        when(response.getStatus()).thenReturn(200);
        logRequestFilter.doFilter(request, response, filterChain);

        verify(filterChain).doFilter(request, response);

        assertThat(filterAppender.list).hasSize(1);
        assertThat(filterAppender.list).allMatch(iLoggingEvent -> iLoggingEvent.getLevel() == Level.DEBUG);
        assertThat(filterAppender.list).allMatch(iLoggingEvent -> iLoggingEvent.getMessage().contains("url=/api/endpoint1"));
        assertThat(filterAppender.list).allMatch(iLoggingEvent -> iLoggingEvent.getMDCPropertyMap().containsKey(MDCConstants.MDC_REQUEST_ID));
        assertThat(filterAppender.list).allMatch(iLoggingEvent -> iLoggingEvent.getMDCPropertyMap().containsKey(MDCConstants.MDC_CALL_ID));
        assertThat(filterAppender.list).allMatch(iLoggingEvent -> iLoggingEvent.getMDCPropertyMap().get(MDCConstants.MDC_CALL_ID).equals("some_call_id"));
        assertThat(filterAppender.list).allMatch(iLoggingEvent -> iLoggingEvent.getMDCPropertyMap().containsKey(MDCConstants.MDC_CONSUMER_ID));
        assertThat(filterAppender.list).allMatch(iLoggingEvent -> iLoggingEvent.getMDCPropertyMap().get(MDCConstants.MDC_CONSUMER_ID).equals("callingApp"));
        assertThat(filterAppender.list).allMatch(iLoggingEvent -> iLoggingEvent.getMDCPropertyMap().containsKey(MDCConstants.MDC_USER_ID));

        verify(response).setHeader(NAV_CALL_ID_HEADER_NAME,"some_call_id");

    }

    @Test
    public void testResponseNotSuccess() throws ServletException, IOException {
        when(response.getStatus()).thenReturn(404);
        logRequestFilter.doFilter(request, response, filterChain);

        verify(filterChain).doFilter(request, response);

        assertThat(filterAppender.list).hasSize(1);
        assertThat(filterAppender.list).allMatch(iLoggingEvent -> iLoggingEvent.getLevel() == Level.WARN);
    }

    @Test
    public void testResponseNeutral() throws ServletException, IOException {
        when(response.getStatus()).thenReturn(302);
        logRequestFilter.doFilter(request, response, filterChain);

        verify(filterChain).doFilter(request, response);

        assertThat(filterAppender.list).hasSize(1);
        assertThat(filterAppender.list).allMatch(iLoggingEvent -> iLoggingEvent.getLevel() == Level.INFO);
    }
}