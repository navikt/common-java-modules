package no.nav.dialogarena.config.ssl;

import lombok.SneakyThrows;

import javax.net.ssl.*;
import java.security.SecureRandom;
import java.security.Security;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;

import static no.nav.dialogarena.config.util.Util.setProperty;

public class SSLTestUtils {

    public static final SSLContext sslContext = trustAllSSLContext();
    public static final SSLSocketFactory TRUST_ALL_SSL_SOCKET_FACTORY = sslContext.getSocketFactory();
    public static final HostnameVerifier ALLOW_ALL_HOSTNAME_VERIFIER = (s, sslSession) -> true;

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
        setProperty("disable.ssl.cn.check", Boolean.TRUE.toString());
        Security.setProperty("ssl.SocketFactory.provider", TrustAllSSLSocketFactory.class.getName());
        CxfCertificateValidationDisabler.init();
        HttpsURLConnection.setDefaultSSLSocketFactory(TRUST_ALL_SSL_SOCKET_FACTORY);
        HttpsURLConnection.setDefaultHostnameVerifier(ALLOW_ALL_HOSTNAME_VERIFIER);
    }

}
