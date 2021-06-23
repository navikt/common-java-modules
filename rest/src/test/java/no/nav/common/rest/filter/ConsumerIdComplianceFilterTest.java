package no.nav.common.rest.filter;

import org.junit.Test;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;

import static org.mockito.Mockito.*;

public class ConsumerIdComplianceFilterTest {

    @Test
    public void should_return_400_if_missing_header_and_enforcing_compliance() throws IOException, ServletException {
        var filter = new ConsumerIdComplianceFilter(true);

        HttpServletRequest request = mock(HttpServletRequest.class);
        HttpServletResponse response = mock(HttpServletResponse.class);
        FilterChain chain = mock(FilterChain.class);

        when(request.getHeader("Nav-Consumer-Id")).thenReturn(null);
        when(response.getWriter()).thenReturn(mock(PrintWriter.class));

        filter.doFilter(request, response, chain);

        verify(response, times(1)).setStatus(400);
        verifyZeroInteractions(chain);
    }

    @Test
    public void should_forward_if_missing_header_and_not_enforcing_compliance() throws IOException, ServletException {
        var filter = new ConsumerIdComplianceFilter(false);

        HttpServletRequest request = mock(HttpServletRequest.class);
        HttpServletResponse response = mock(HttpServletResponse.class);
        FilterChain chain = mock(FilterChain.class);

        when(request.getHeader("Nav-Consumer-Id")).thenReturn(null);

        filter.doFilter(request, response, chain);

        verifyZeroInteractions(response);
        verify(chain, times(1)).doFilter(request, response);
    }

    @Test
    public void should_forward_if_request_has_consumer_id_and_enforcing_compliance() throws IOException, ServletException {
        var filter = new ConsumerIdComplianceFilter(true);

        HttpServletRequest request = mock(HttpServletRequest.class);
        HttpServletResponse response = mock(HttpServletResponse.class);
        FilterChain chain = mock(FilterChain.class);

        when(request.getHeader("Nav-Consumer-Id")).thenReturn("some-application");

        filter.doFilter(request, response, chain);

        verifyZeroInteractions(response);
        verify(chain, times(1)).doFilter(request, response);
    }

}
