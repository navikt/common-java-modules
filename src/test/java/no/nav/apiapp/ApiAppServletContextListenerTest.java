package no.nav.apiapp;


import org.junit.Test;

import javax.servlet.ServletContext;

import static no.nav.apiapp.ApiAppServletContextListener.getContextName;
import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class ApiAppServletContextListenerTest {

    @Test
    public void getContextName__bruk_contextPath_hvis_denne_finnes() {
        ApiApplication apiApplication = mock(ApiApplication.class);
        when(apiApplication.getApplicationName()).thenReturn("app-name");

        assertThat(getContextName(servletContext(null), apiApplication)).isEqualTo("app-name");
        assertThat(getContextName(servletContext(""), apiApplication)).isEqualTo("app-name");
        assertThat(getContextName(servletContext("/"), apiApplication)).isEqualTo("app-name");
        assertThat(getContextName(servletContext("/context-a"), apiApplication)).isEqualTo("context-a");
        assertThat(getContextName(servletContext("/context-b"), apiApplication)).isEqualTo("context-b");
    }

    private ServletContext servletContext(String contextPath) {
        ServletContext servletContext = mock(ServletContext.class);
        when(servletContext.getContextPath()).thenReturn(contextPath);
        return servletContext;
    }

}