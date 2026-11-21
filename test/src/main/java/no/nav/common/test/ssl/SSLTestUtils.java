package no.nav.common.test.ssl;

import java.io.IOException;
import java.io.InputStream;
import java.security.GeneralSecurityException;
import java.security.KeyStore;
import java.security.cert.CertificateFactory;
import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManager;
import javax.net.ssl.TrustManagerFactory;
import javax.net.ssl.X509TrustManager;

import org.apache.cxf.transport.https.httpclient.DefaultHostnameVerifier;

public class SSLTestUtils {
    public static final SSLContext sslContext = createSSLContext();

    @SuppressWarnings("deprecation") // Adapteren legger vertsnavnkontroll til alle socket-overlastene.
    public static final SSLSocketFactory TEST_SSL_SOCKET_FACTORY = new TrustAllSSLSocketFactory();

    public static final HostnameVerifier TEST_HOSTNAME_VERIFIER = new DefaultHostnameVerifier();

    /**
     * Stoler kun på testsertifikatet og kontrollerer host.
     *
     * @deprecated Bruk {@link #TEST_SSL_SOCKET_FACTORY}. Navnet beholdes for kompatibilitet, men gir ikke trust-all.
     */
    @Deprecated
    public static final SSLSocketFactory TRUST_ALL_SSL_SOCKET_FACTORY = TEST_SSL_SOCKET_FACTORY;

    /**
     * Kontrollerer host mot sertifikatet.
     *
     * @deprecated Bruk {@link #TEST_HOSTNAME_VERIFIER}. Navnet beholdes for kompatibilitet, men godtar ikke vilkårlige hosts.
     */
    @Deprecated
    public static final HostnameVerifier ALLOW_ALL_HOSTNAME_VERIFIER = TEST_HOSTNAME_VERIFIER;

    private SSLTestUtils() {
    }

    public static SSLContext createSSLContext() {
        return createSSLContext(fixtureTrustStore());
    }

    public static SSLContext createSSLContext(KeyStore trustStore) {
        if (trustStore == null) {
            throw new IllegalArgumentException("Et eksplisitt tillitslager er påkrevd");
        }
        try {
            var factory = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
            factory.init(trustStore);
            var context = SSLContext.getInstance("TLS");
            context.init(null, factory.getTrustManagers(), null);
            return context;
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException("Kunne ikke opprette TLS-kontekst for test", e);
        }
    }

    /**
     * Avviser forsøk på globale TLS-endringer.
     *
     * @deprecated Konfigurer TLS på testklienten med {@link #createSSLContext(KeyStore)}. Metoden kaster alltid
     * {@link UnsupportedOperationException}.
     */
    @Deprecated
    public static void disableCertificateChecks() {
        throw new UnsupportedOperationException("Konfigurer TLS eksplisitt på testklienten med createSSLContext()");
    }

    /**
     * Avviser forsøk på å sette globale tillitslagre.
     *
     * @deprecated Konfigurer TLS på testklienten med {@link #createSSLContext(KeyStore)} og last testserverens keystore
     * direkte. Metoden kaster alltid {@link UnsupportedOperationException}.
     */
    @Deprecated
    public static void setupKeyAndTrustStore() {
        throw new UnsupportedOperationException("Konfigurer TLS eksplisitt på testklienten med createSSLContext()");
    }

    static X509TrustManager fixtureTrustManager() {
        try {
            var factory = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
            factory.init(fixtureTrustStore());
            for (TrustManager manager : factory.getTrustManagers()) {
                if (manager instanceof X509TrustManager) {
                    return (X509TrustManager) manager;
                }
            }
            throw new IllegalStateException("Fant ingen X509-tillitsbehandler");
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException("Kunne ikke laste testsertifikatet", e);
        }
    }

    private static KeyStore fixtureTrustStore() {
        try (InputStream input = SSLTestUtils.class.getResourceAsStream("/dummy.crt")) {
            if (input == null) {
                throw new IllegalStateException("Fant ikke testsertifikatet /dummy.crt");
            }
            var trustStore = KeyStore.getInstance("JKS");
            trustStore.load(null, null);
            trustStore.setCertificateEntry(
                "localhost", CertificateFactory.getInstance("X.509").generateCertificate(input));
            return trustStore;
        } catch (GeneralSecurityException | IOException e) {
            throw new IllegalStateException("Kunne ikke laste tillitslageret for test", e);
        }
    }
}
