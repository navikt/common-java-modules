package no.nav.dialogarena.config.ssl;

import lombok.SneakyThrows;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManager;
import java.io.IOException;
import java.net.InetAddress;
import java.net.Socket;
import java.net.UnknownHostException;
import java.security.SecureRandom;

import static no.nav.dialogarena.config.ssl.SSLTestUtils.TRUST_ALL_SSL_SOCKET_FACTORY;

public class TrustAllSSLSocketFactory extends SSLSocketFactory {

    @Override
    public Socket createSocket() throws IOException {
        return TRUST_ALL_SSL_SOCKET_FACTORY.createSocket();
    }

    @Override
    public Socket createSocket(String s, int i) throws IOException, UnknownHostException {
        return TRUST_ALL_SSL_SOCKET_FACTORY.createSocket(s, i);
    }

    @Override
    public Socket createSocket(String s, int i, InetAddress inetAddress, int i1) throws IOException, UnknownHostException {
        return TRUST_ALL_SSL_SOCKET_FACTORY.createSocket(s, i, inetAddress, i1);
    }

    @Override
    public Socket createSocket(InetAddress inetAddress, int i) throws IOException {
        return TRUST_ALL_SSL_SOCKET_FACTORY.createSocket(inetAddress, i);
    }

    @Override
    public Socket createSocket(InetAddress inetAddress, int i, InetAddress inetAddress1, int i1) throws IOException {
        return TRUST_ALL_SSL_SOCKET_FACTORY.createSocket(inetAddress, i, inetAddress1, i1);
    }

    @Override
    public String[] getDefaultCipherSuites() {
        return new String[0];
    }

    @Override
    public String[] getSupportedCipherSuites() {
        return new String[0];
    }

    @Override
    public Socket createSocket(Socket socket, String s, int i, boolean b) throws IOException {
        return TRUST_ALL_SSL_SOCKET_FACTORY.createSocket(socket, s, i, b);
    }

}
