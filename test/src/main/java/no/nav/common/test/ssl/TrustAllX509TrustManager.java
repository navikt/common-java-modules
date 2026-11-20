package no.nav.common.test.ssl;

import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import javax.net.ssl.X509TrustManager;

/**
 * Stoler kun på det medfølgende testsertifikatet.
 *
 * @deprecated Konfigurer testklienten med {@link SSLTestUtils#createSSLContext(java.security.KeyStore)}.
 *     Klassen beholdes for kompatibilitet og gir ikke trust-all.
 */
@Deprecated
public class TrustAllX509TrustManager implements X509TrustManager {
    private final X509TrustManager delegate = SSLTestUtils.fixtureTrustManager();

    @Override
    public void checkClientTrusted(X509Certificate[] x509Certificates, String s) throws CertificateException {
        delegate.checkClientTrusted(x509Certificates, s);
    }

    @Override
    public void checkServerTrusted(X509Certificate[] x509Certificates, String s) throws CertificateException {
        delegate.checkServerTrusted(x509Certificates, s);
    }

    @Override
    public X509Certificate[] getAcceptedIssuers() {
        return delegate.getAcceptedIssuers();
    }
}
