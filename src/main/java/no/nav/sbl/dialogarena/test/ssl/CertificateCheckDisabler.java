package no.nav.sbl.dialogarena.test.ssl;

import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;

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
