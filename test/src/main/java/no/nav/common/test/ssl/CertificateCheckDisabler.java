package no.nav.common.test.ssl;

import jakarta.servlet.ServletContextEvent;
import jakarta.servlet.ServletContextListener;

/*
 Kan brukes til å disable SSL i riktig/nødvendig class-context for en webapp, altså for webappens classloader
*/
@SuppressWarnings("unused")
public class CertificateCheckDisabler implements ServletContextListener {

    @Override
    public void contextInitialized(ServletContextEvent sce) {
        SSLTestUtils.disableCertificateChecks();
    }

    @Override
    public void contextDestroyed(ServletContextEvent sce) {

    }

}
