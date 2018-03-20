package no.nav.brukerdialog.security.jaspic;

import org.junit.Test;

import javax.servlet.ServletContext;
import javax.servlet.ServletContextEvent;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class OidcAuthRegistrationTest {

    @Test
    public void smoketest() {
        ServletContextEvent servletContextEvent = mock(ServletContextEvent.class);
        when(servletContextEvent.getServletContext()).thenReturn(mock(ServletContext.class));
        OidcAuthRegistration.registerOidcAuthModule(servletContextEvent, true);
    }

}