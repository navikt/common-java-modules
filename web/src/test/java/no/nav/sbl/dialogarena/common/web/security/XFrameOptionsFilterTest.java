package no.nav.sbl.dialogarena.common.web.security;

import no.nav.sbl.dialogarena.common.web.security.XFrameOptionsFilter;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

import static no.nav.sbl.dialogarena.common.web.security.XFrameOptionsFilter.DEFAULT_OPTION;
import static no.nav.sbl.dialogarena.common.web.security.XFrameOptionsFilter.OPTION_INIT_PARAMETER_NAME;
import static no.nav.sbl.dialogarena.common.web.security.XFrameOptionsFilter.SAMEORIGIN_OPTION;
import static no.nav.sbl.dialogarena.common.web.security.XFrameOptionsFilter.X_FRAME_OPTIONS_HEADER_NAME;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;

public class XFrameOptionsFilterTest {

    @Mock
    private FilterConfig mockFilterConfig;

    @Mock
    private HttpServletResponse mockHttpServletResponse;

    @Mock
    private FilterChain mockFilterChain;

    private XFrameOptionsFilter filter;

    @Before
    public void setUp() {
        initMocks(this);
        filter = new XFrameOptionsFilter();
    }

    @Test
    public void skal_initialisere_filter_med_default_option_om_ikke_angitt() throws ServletException, IOException {
        when(mockFilterConfig.getInitParameter(OPTION_INIT_PARAMETER_NAME)).thenReturn(null);
        filter.init(mockFilterConfig);
        filter.doFilter(null, mockHttpServletResponse, mockFilterChain);
        verify(mockHttpServletResponse, times(1)).setHeader(X_FRAME_OPTIONS_HEADER_NAME, DEFAULT_OPTION);
    }

    @Test
    public void skal_initialisere_filter_med_angitt_option_dersom_den_er_gyldig() throws ServletException, IOException {
        when(mockFilterConfig.getInitParameter(OPTION_INIT_PARAMETER_NAME)).thenReturn(SAMEORIGIN_OPTION);
        filter.init(mockFilterConfig);
        filter.doFilter(null, mockHttpServletResponse, mockFilterChain);
        verify(mockHttpServletResponse, times(1)).setHeader(X_FRAME_OPTIONS_HEADER_NAME, SAMEORIGIN_OPTION);
    }

    @Test(expected = IllegalArgumentException.class)
    public void skal_ikke_initialisere_filter_med_angitt_option_dersom_den_er_ugyldig() throws ServletException, IOException {
        when(mockFilterConfig.getInitParameter(OPTION_INIT_PARAMETER_NAME)).thenReturn("UGYLDIG VERDI");
        filter.init(mockFilterConfig);
    }

}
