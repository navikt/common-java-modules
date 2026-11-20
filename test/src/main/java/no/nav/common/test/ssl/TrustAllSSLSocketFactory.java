package no.nav.common.test.ssl;

import java.io.IOException;
import java.net.InetAddress;
import java.net.Socket;
import javax.net.ssl.SSLParameters;
import javax.net.ssl.SSLSocket;
import javax.net.ssl.SSLSocketFactory;

/**
 * Validerer testsertifikatet og host.
 *
 * @deprecated Bruk {@link SSLTestUtils#TEST_SSL_SOCKET_FACTORY} eller konfigurer klienten med
 *     {@link SSLTestUtils#createSSLContext(java.security.KeyStore)}. Klassen beholdes for kompatibilitet.
 */
@Deprecated
public class TrustAllSSLSocketFactory extends SSLSocketFactory {
    private final SSLSocketFactory delegate = SSLTestUtils.sslContext.getSocketFactory();

    @Override
    public Socket createSocket() throws IOException {
        return verifyHostname(delegate.createSocket());
    }

    @Override
    public Socket createSocket(String s, int i) throws IOException {
        return verifyHostname(delegate.createSocket(s, i));
    }

    @Override
    public Socket createSocket(String s, int i, InetAddress inetAddress, int i1) throws IOException {
        return verifyHostname(delegate.createSocket(s, i, inetAddress, i1));
    }

    @Override
    public Socket createSocket(InetAddress inetAddress, int i) throws IOException {
        return verifyHostname(delegate.createSocket(inetAddress, i));
    }

    @Override
    public Socket createSocket(InetAddress inetAddress, int i, InetAddress inetAddress1, int i1) throws IOException {
        return verifyHostname(delegate.createSocket(inetAddress, i, inetAddress1, i1));
    }

    @Override
    public String[] getDefaultCipherSuites() {
        return delegate.getDefaultCipherSuites();
    }

    @Override
    public String[] getSupportedCipherSuites() {
        return delegate.getSupportedCipherSuites();
    }

    @Override
    public Socket createSocket(Socket socket, String s, int i, boolean b) throws IOException {
        return verifyHostname(delegate.createSocket(socket, s, i, b));
    }

    private Socket verifyHostname(Socket socket) {
        SSLSocket sslSocket = (SSLSocket) socket;
        SSLParameters parameters = sslSocket.getSSLParameters();
        parameters.setEndpointIdentificationAlgorithm("HTTPS");
        sslSocket.setSSLParameters(parameters);
        return sslSocket;
    }
}
