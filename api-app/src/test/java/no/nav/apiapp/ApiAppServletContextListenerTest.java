package no.nav.apiapp;


import no.nav.sbl.dialogarena.test.junit.SystemPropertiesRule;
import org.junit.Rule;
import org.junit.Test;

import javax.servlet.ServletContext;

import static no.nav.apiapp.ApiAppServletContextListener.getContextName;
import static no.nav.sbl.util.EnvironmentUtils.APP_NAME_PROPERTY_NAME;
import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class ApiAppServletContextListenerTest {

    @Rule
    public SystemPropertiesRule systemPropertiesRule = new SystemPropertiesRule();

    @Test
    public void getContextName__bruk_contextPath_hvis_denne_finnes() {
        ApiApplication apiApplication = mock(ApiApplication.class);

        systemPropertiesRule.setProperty(APP_NAME_PROPERTY_NAME, "app-name");

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