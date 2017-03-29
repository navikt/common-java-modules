package no.nav.brukerdialog.security.jaspic;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.security.auth.message.config.AuthConfigFactory;
import javax.security.auth.message.module.ServerAuthModule;
import javax.servlet.ServletContext;
import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;
import javax.servlet.annotation.WebListener;

@WebListener
public class SamAutoRegistration implements ServletContextListener {
    public static final String CONTEXT_REGISTRATION_ID = "no.nav.kes.security.jaspic.security.message.registrationId";
    private static final Logger log = LoggerFactory.getLogger(SamAutoRegistration.class);


    public SamAutoRegistration() {
        log.debug("Instancieted");
    }

    @Override
    public void contextInitialized(ServletContextEvent sce) {
        log.debug("Initializing JASPIC");
        registerServerAuthModule(new OidcAuthModule(), sce.getServletContext());
    }

    @Override
    public void contextDestroyed(ServletContextEvent sce) {
        deregisterServerAuthModule(sce.getServletContext());
    }

    /**
     * Registers a server auth module as the one and only module for the application corresponding to
     * the given servlet context.
     * <p>
     * <p>
     * This will override any other modules that have already been registered, either via proprietary
     * means or using the standard API.
     *
     * @param serverAuthModule the server auth module to be registered
     * @param servletContext   the context of the app for which the module is registered
     * @return A String identifier assigned by an underlying factory corresponding to an underlying factory-factory-factory registration
     */
    public static String registerServerAuthModule(ServerAuthModule serverAuthModule, ServletContext servletContext) {

        // Register the factory-factory-factory for the SAM
        String registrationId = AuthConfigFactory.getFactory().registerConfigProvider(
                new OidcAuthConfigProvider(serverAuthModule),
                "HttpServlet",
                getAppContextID(servletContext),
                "Default single SAM authentication config provider"
        );

        // Remember the registration ID returned by the factory, so we can unregister the JASPIC module when the web module
        // is undeployed. JASPIC being the low level API that it is won't do this automatically.
        servletContext.setAttribute(CONTEXT_REGISTRATION_ID, registrationId);

        return registrationId;
    }

    /**
     * Deregisters the server auth module (and encompassing wrappers/factories) that was previously registered via a call
     * to registerServerAuthModule.
     *
     * @param servletContext the context of the app for which the module is deregistered
     */
    public static void deregisterServerAuthModule(ServletContext servletContext) {
        String registrationId = (String) servletContext.getAttribute(CONTEXT_REGISTRATION_ID);
        if (!isEmpty(registrationId)) {
            AuthConfigFactory.getFactory().removeRegistration(registrationId);
        }
    }

    /**
     * Gets the app context ID from the servlet context.
     * <p>
     * <p>
     * The app context ID is the ID that JASPIC associates with the given application.
     * In this case that given application is the web application corresponding to the
     * ServletContext.
     *
     * @param context the servlet context for which to obtain the JASPIC app context ID
     * @return the app context ID for the web application corresponding to the given context
     */
    public static String getAppContextID(ServletContext context) {
        return context.getVirtualServerName() + " " + context.getContextPath();
    }

    /**
     * Returns true if the given string is null or is empty.
     *
     * @param string The string to be checked on emptiness.
     * @return True if the given string is null or is empty.
     */
    public static boolean isEmpty(String string) {
        return string == null || string.isEmpty();
    }
}
