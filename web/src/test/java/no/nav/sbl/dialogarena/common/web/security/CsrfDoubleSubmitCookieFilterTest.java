package no.nav.sbl.dialogarena.common.web.security;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.UUID;

import static org.mockito.Mockito.*;
import static org.mockito.MockitoAnnotations.initMocks;

public class CsrfDoubleSubmitCookieFilterTest {

    private static final String NAV_CSRF_PROTECTION = "NAV_CSRF_PROTECTION";

    @Mock
    private FilterConfig mockFilterConfig;

    @Mock
    private HttpServletRequest mockHttpServletRequest;

    @Mock
    private HttpServletResponse mockHttpServletResponse;

    @Mock
    private FilterChain mockFilterChain;

    private CsrfDoubleSubmitCookieFilter filter;

    @Before
    public void setUp() throws ServletException {
        initMocks(this);
        filter = new CsrfDoubleSubmitCookieFilter();
        filter.init(mockFilterConfig);

        when(mockHttpServletRequest.getRequestURI()).thenReturn("");
        when(mockHttpServletRequest.getContextPath()).thenReturn("");

        Cookie chocolateCookie = createCsrfProtectionCookie();
        when(mockHttpServletRequest.getCookies()).thenReturn(new Cookie[]{chocolateCookie});
        when(mockHttpServletRequest.getHeader(NAV_CSRF_PROTECTION)).thenReturn(chocolateCookie.getValue());
    }

    @Test
    public void skal_ikke_filtrere_gyldige_csrf_state_endring_requests() throws ServletException, IOException {
        filter.doFilter(mockHttpServletRequest, mockHttpServletResponse, mockFilterChain);

        verify(mockHttpServletResponse, times(0)).sendError(anyInt(), anyString());
        verify(mockFilterChain, times(1)).doFilter(mockHttpServletRequest, mockHttpServletResponse);
    }

    @Test
    public void skal_ikke_filtrere_ugyldige_csrf_get_requests() throws ServletException, IOException {
        when(mockHttpServletRequest.getMethod()).thenReturn("GET");
        when(mockHttpServletRequest.getHeader(NAV_CSRF_PROTECTION)).thenReturn("Dette er en ugyldig header");

        filter.doFilter(mockHttpServletRequest, mockHttpServletResponse, mockFilterChain);

        verify(mockHttpServletResponse, times(0)).sendError(anyInt(), anyString());
        verify(mockFilterChain, times(1)).doFilter(mockHttpServletRequest, mockHttpServletResponse);
    }

    @Test
    public void skal_ikke_filtrere_ugyldige_csrf_head_requests() throws ServletException, IOException {
        when(mockHttpServletRequest.getMethod()).thenReturn("HEAD");
        when(mockHttpServletRequest.getHeader(NAV_CSRF_PROTECTION)).thenReturn("Dette er en ugyldig header");

        filter.doFilter(mockHttpServletRequest, mockHttpServletResponse, mockFilterChain);

        verify(mockHttpServletResponse, times(0)).sendError(anyInt(), anyString());
        verify(mockFilterChain, times(1)).doFilter(mockHttpServletRequest, mockHttpServletResponse);
    }

    @Test
    public void skal_filtrere_ugyldige_csrf_state_endring_requests_og_gi_feilmelding() throws ServletException, IOException {
        when(mockHttpServletRequest.getHeader(NAV_CSRF_PROTECTION)).thenReturn("Dette er en ugyldig header");
        filter.doFilter(mockHttpServletRequest, mockHttpServletResponse, mockFilterChain);

        verify(mockHttpServletResponse, times(1)).sendError(anyInt(), anyString());
        verify(mockFilterChain, times(0)).doFilter(any(), any());
    }

    @Test
    public void skal_kaste_feil_og_avbryte_dersom_header_mangler() throws IOException, ServletException {
        when(mockHttpServletRequest.getHeader(NAV_CSRF_PROTECTION)).thenReturn("");

        filter.doFilter(mockHttpServletRequest, mockHttpServletResponse, mockFilterChain);
        verify(mockFilterChain, never()).doFilter(mockHttpServletRequest, mockHttpServletResponse);
    }

    @Test
    public void skal_kaste_feil_og_avbryte_dersom_header_ikke_matcher_cookieverdi() throws IOException, ServletException {
        when(mockHttpServletRequest.getHeader(NAV_CSRF_PROTECTION)).thenReturn("Matcher ikke");

        filter.doFilter(mockHttpServletRequest, mockHttpServletResponse, mockFilterChain);
        verify(mockFilterChain, never()).doFilter(mockHttpServletRequest, mockHttpServletResponse);
    }

    @Test
    public void takler_at_ingen_cookies_er_satt() throws ServletException, IOException {
        when(mockHttpServletRequest.getCookies()).thenReturn(null);

        filter.doFilter(mockHttpServletRequest, mockHttpServletResponse, mockFilterChain);

        verify(mockHttpServletResponse).sendError(anyInt(), anyString());
        verify(mockFilterChain, never()).doFilter(mockHttpServletRequest, mockHttpServletResponse);
    }

    private Cookie createCsrfProtectionCookie() {
        return new Cookie(NAV_CSRF_PROTECTION, UUID.randomUUID().toString());
    }
}
