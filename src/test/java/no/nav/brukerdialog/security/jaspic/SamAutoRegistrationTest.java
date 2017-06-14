package no.nav.brukerdialog.security.jaspic;

import org.jboss.security.SecurityContext;
import org.jboss.security.SecurityContextAssociation;
import org.junit.Before;
import org.junit.Test;

import javax.security.auth.message.config.AuthConfigFactory;
import javax.servlet.ServletContext;
import javax.servlet.ServletContextEvent;

import static no.nav.brukerdialog.security.jaspic.SamAutoRegistration.AUTO_REGISTRATION_PROPERTY_NAME;
import static no.nav.brukerdialog.security.jaspic.SamAutoRegistration.JASPI_SECURITY_DOMAIN;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;


public class SamAutoRegistrationTest {

    private ServletContext servletContext = mock(ServletContext.class);
    private AuthConfigFactory authConfigFactory = mock(AuthConfigFactory.class);
    private SecurityContext securityContext = mock(SecurityContext.class);

    @Before
    public void setup() {
        AuthConfigFactory.setFactory(authConfigFactory);
        SecurityContextAssociation.setSecurityContext(securityContext);
        when(servletContext.getContextPath()).thenReturn("/contextPath");
        when(servletContext.getVirtualServerName()).thenReturn("virtualServerName");
    }

    @Test
    public void contextInitialized_default_noAutomaticRegistration() {
        initializeContext();
        assertNoAutomaticRegistration();
    }

    @Test
    public void contextInitialized_jaspiSecurityDomain_registration() {
        when(securityContext.getSecurityDomain()).thenReturn(JASPI_SECURITY_DOMAIN);
        initializeContext();
        assertAutomaticRegistration();
    }

    @Test
    public void contextInitialized_initParameter_registration() {
        when(servletContext.getInitParameter(AUTO_REGISTRATION_PROPERTY_NAME)).thenReturn(Boolean.TRUE.toString());
        initializeContext();
        assertAutomaticRegistration();
    }

    private void initializeContext() {
        new SamAutoRegistration().contextInitialized(new ServletContextEvent(servletContext));
    }

    private void assertNoAutomaticRegistration() {
        verifyZeroInteractions(authConfigFactory);
    }

    private void assertAutomaticRegistration() {
        verify(authConfigFactory).registerConfigProvider(isA(OidcAuthConfigProvider.class), eq("HttpServlet"), eq("virtualServerName /contextPath"), anyString());
    }

}
