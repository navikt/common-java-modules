package no.nav.sbl.dialogarena.test.ssl;

import lombok.SneakyThrows;
import no.nav.modig.testcertificates.TestCertificates;
import org.apache.http.conn.ssl.X509HostnameVerifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.net.ssl.*;
import java.io.IOException;
import java.security.SecureRandom;
import java.security.Security;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;

import static java.lang.System.setProperty;

public class SSLTestUtils {
    private static final Logger LOG = LoggerFactory.getLogger(SSLTestUtils.class);

    public static final SSLContext sslContext = trustAllSSLContext();
    public static final SSLSocketFactory TRUST_ALL_SSL_SOCKET_FACTORY = sslContext.getSocketFactory();
    public static final X509HostnameVerifier ALLOW_ALL_X509_HOSTNAME_VERIFIER = new X509HostnameVerifier() {
        @Override
        public void verify(String host, SSLSocket ssl) throws IOException {

        }

        @Override
        public void verify(String host, X509Certificate cert) throws SSLException {

        }

        @Override
        public void verify(String host, String[] cns, String[] subjectAlts) throws SSLException {

        }

        @Override
        public boolean verify(String s, SSLSession sslSession) {
            return true;
        }
    };
    public static final HostnameVerifier ALLOW_ALL_HOSTNAME_VERIFIER = ALLOW_ALL_X509_HOSTNAME_VERIFIER;

    @SneakyThrows
    private static SSLContext trustAllSSLContext() {
        SSLContext sslContext = SSLContext.getInstance("SSL");
        sslContext.init(null, new TrustManager[]{
                new X509TrustManager() {
                    @Override
                    public void checkClientTrusted(X509Certificate[] x509Certificates, String s) throws CertificateException {

                    }

                    @Override
                    public void checkServerTrusted(X509Certificate[] x509Certificates, String s) throws CertificateException {
                    }

                    @Override
                    public X509Certificate[] getAcceptedIssuers() {
                        return null;
                    }
                }
        }, new SecureRandom());
        return sslContext;
    }

    /*
    Bruk alle triks i boka for å disable sertifikat-sjekker
     */
    public static void disableCertificateChecks() {
        setupKeyAndTrustStore();
        setProperty("disable.ssl.cn.check", Boolean.TRUE.toString());
        Security.setProperty("ssl.SocketFactory.provider", TrustAllSSLSocketFactory.class.getName());
        CxfCertificateValidationDisabler.init();
        HttpsURLConnection.setDefaultSSLSocketFactory(TRUST_ALL_SSL_SOCKET_FACTORY);
        HttpsURLConnection.setDefaultHostnameVerifier(ALLOW_ALL_HOSTNAME_VERIFIER);
        SSLContext.setDefault(sslContext);
        systemPropertyObject(SSLContext.class, sslContext);
        systemPropertyObject(HostnameVerifier.class, ALLOW_ALL_HOSTNAME_VERIFIER);
        systemPropertyObject(X509HostnameVerifier.class, ALLOW_ALL_X509_HOSTNAME_VERIFIER);
    }

    private static void setupKeyAndTrustStore() {
        try {
            TestCertificates.setupKeyAndTrustStore();
        } catch (NoClassDefFoundError noClassDefFoundError) {
            // siden både common-test og modig-testcertificates ofte ligger i test-scope, inkluderes ikke alltid
            // modig-testcertificates og dens transitive avhengigheter korrekt på klasspathen.
            // samtidig er det et gyldig case å kalle disableCertificateChecks() uten at setupKeyAndTrustStore() skjer!
            LOG.warn("kunne ikke konfigurere test key- og truststore. Mangler modig-klasser på classpath");
        }
    }

    private static <T> void systemPropertyObject(Class<T> aClass, T value) {
        System.getProperties().put(aClass.getName(), value);
    }

}
