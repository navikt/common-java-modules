package no.nav.common.test.ssl;

import jakarta.servlet.ServletContextEvent;
import jakarta.servlet.ServletContextListener;

/**
 * Beholdt for kompatibilitet med eksisterende servlet-oppsett.
 *
 * @deprecated Globale TLS-endringer støttes ikke. Fjern listeneren og konfigurer TLS eksplisitt på testklienten.
 */
@Deprecated
@SuppressWarnings("unused")
public class CertificateCheckDisabler implements ServletContextListener {

    @Override
    public void contextInitialized(ServletContextEvent sce) {
        SSLTestUtils.disableCertificateChecks();
    }
}
