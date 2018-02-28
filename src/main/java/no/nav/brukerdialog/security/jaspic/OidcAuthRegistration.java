package no.nav.brukerdialog.security.jaspic;

import org.apache.geronimo.components.jaspi.AuthConfigFactoryImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.security.auth.message.config.AuthConfigFactory;
import javax.security.auth.message.module.ServerAuthModule;
import javax.servlet.ServletContext;
import javax.servlet.ServletContextEvent;

import static java.lang.System.setProperty;

public class OidcAuthRegistration {
    private static final Logger log = LoggerFactory.getLogger(OidcAuthRegistration.class);

    public static void registerOidcAuthModule(ServletContextEvent sce, boolean statelessApplication) {
        registerOidcAuthModule(new OidcAuthModule(statelessApplication), sce.getServletContext());
    }

    public static void registerOidcAuthModule(ServerAuthModule serverAuthModule, ServletContext servletContext) {
        setProperty(AuthConfigFactory.DEFAULT_FACTORY_SECURITY_PROPERTY, AuthConfigFactoryImpl.class.getCanonicalName());
        String registrationId = AuthConfigFactory.getFactory().registerConfigProvider(
                new OidcAuthConfigProvider(serverAuthModule),
                "HTTP",
                getAppContextID(servletContext),
                "Default single SAM authentication config provider"
        );
        log.info("oidc auth module registered on [{}] for servlet context [{}]", registrationId, servletContext);
    }

    public static String getAppContextID(ServletContext context) {
        // NB: dette må korrespondere med implementasjonen i
        // org.eclipse.jetty.security.jaspi.JaspiAuthenticatorFactory.getAuthenticator()
        return "server" + " " + context.getContextPath();
    }
}
