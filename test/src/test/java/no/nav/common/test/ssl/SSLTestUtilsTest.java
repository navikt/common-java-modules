package no.nav.common.test.ssl;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.sun.net.httpserver.HttpsConfigurator;
import com.sun.net.httpserver.HttpsServer;
import java.io.InputStream;
import java.net.InetSocketAddress;
import java.net.URL;
import java.security.KeyStore;
import java.security.Security;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.Properties;
import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLHandshakeException;
import javax.net.ssl.SSLSocket;
import javax.net.ssl.SSLSocketFactory;
import org.apache.cxf.Bus;
import org.apache.cxf.configuration.Configurer;
import org.apache.cxf.configuration.jsse.TLSClientParameters;
import org.apache.cxf.endpoint.Client;
import org.apache.cxf.transport.http.HTTPConduit;
import org.junit.After;
import org.junit.Test;
import org.mockito.ArgumentCaptor;

public class SSLTestUtilsTest {

    private HttpsServer server;

    @After
    public void stopp_server() {
        if (server != null) {
            server.stop(0);
        }
    }

    @Test
    public void godtar_testsertifikatet_for_riktig_vertsnavn() throws Exception {
        startServer("/dummy.jks");

        assertEquals(200, status("localhost", SSLTestUtils.TEST_SSL_SOCKET_FACTORY));
        assertEquals(200, status("localhost", SSLTestUtils.createSSLContext().getSocketFactory()));
    }

    @Test
    public void avviser_sertifikat_utenfor_tillitslageret() throws Exception {
        startServer("/untrusted.jks");

        assertThrows(SSLHandshakeException.class, () -> status("localhost", SSLTestUtils.TEST_SSL_SOCKET_FACTORY));
    }

    @Test
    public void avviser_feil_vertsnavn_selv_om_sertifikatet_er_betrodd() throws Exception {
        startServer("/dummy.jks");

        assertThrows(SSLHandshakeException.class, () -> status("127.0.0.1", SSLTestUtils.TEST_SSL_SOCKET_FACTORY));
    }

    @Test
    public void den_gamle_socketfabrikken_validerer_vertsnavn_og_sertifikat() throws Exception {
        startServer("/dummy.jks");
        SSLSocketFactory factory = new TrustAllSSLSocketFactory();

        assertTrue(factory.getDefaultCipherSuites().length > 0);
        assertTrue(factory.getSupportedCipherSuites().length > 0);
        try (SSLSocket socket = (SSLSocket)
                factory.createSocket("localhost", server.getAddress().getPort())) {
            socket.setSoTimeout(5000);
            socket.startHandshake();
            assertEquals("HTTPS", socket.getSSLParameters().getEndpointIdentificationAlgorithm());
            assertTrue(SSLTestUtils.ALLOW_ALL_HOSTNAME_VERIFIER.verify("localhost", socket.getSession()));
            assertFalse(SSLTestUtils.ALLOW_ALL_HOSTNAME_VERIFIER.verify("127.0.0.1", socket.getSession()));
        }
        try (SSLSocket socket = (SSLSocket)
                factory.createSocket("127.0.0.1", server.getAddress().getPort())) {
            socket.setSoTimeout(5000);
            assertThrows(SSLHandshakeException.class, socket::startHandshake);
        }
    }

    @Test
    public void den_gamle_tillitsbehandleren_avviser_ukjente_klienter_og_servere() throws Exception {
        TrustAllX509TrustManager manager = new TrustAllX509TrustManager();
        X509Certificate[] trusted = certificateChain("/dummy.jks");
        X509Certificate[] untrusted = certificateChain("/untrusted.jks");

        manager.checkServerTrusted(trusted, "RSA");
        manager.checkClientTrusted(trusted, "RSA");
        assertTrue(manager.getAcceptedIssuers().length > 0);
        assertThrows(CertificateException.class, () -> manager.checkServerTrusted(untrusted, "RSA"));
        assertThrows(CertificateException.class, () -> manager.checkClientTrusted(untrusted, "RSA"));
    }

    @Test
    public void bruker_kun_det_eksplisitte_tillitslageret() throws Exception {
        startServer("/untrusted.jks");
        KeyStore trustStore = KeyStore.getInstance("JKS");
        trustStore.load(null, null);
        trustStore.setCertificateEntry("annen-testserver", certificateChain("/untrusted.jks")[0]);

        assertEquals(
                200,
                status("localhost", SSLTestUtils.createSSLContext(trustStore).getSocketFactory()));
        assertThrows(IllegalArgumentException.class, () -> SSLTestUtils.createSSLContext(null));
    }

    @Test
    public void cxf_beholder_sertifikatkontroll_og_vertsnavnkontroll() {
        Client client = mock(Client.class);
        HTTPConduit conduit = mock(HTTPConduit.class);
        when(client.getConduit()).thenReturn(conduit);

        new CxfCertificateValidationDisabler().initialize(client, mock(Bus.class));

        ArgumentCaptor<TLSClientParameters> parameters = ArgumentCaptor.forClass(TLSClientParameters.class);
        verify(conduit).setTlsClientParameters(parameters.capture());
        assertFalse(parameters.getValue().isDisableCNCheck());
        assertFalse(parameters.getValue().isUseHttpsURLConnectionDefaultHostnameVerifier());
        assertSame(SSLTestUtils.TEST_SSL_SOCKET_FACTORY, parameters.getValue().getSSLSocketFactory());
        assertSame(SSLTestUtils.TEST_HOSTNAME_VERIFIER, parameters.getValue().getHostnameVerifier());
        assertNotNull(parameters.getValue().getTrustManagers());
    }

    @Test
    public void cxf_konfigurerer_kun_den_angitte_bussen_uten_saml_omgaaelse() {
        Bus bus = mock(Bus.class);
        Configurer original = mock(Configurer.class);
        when(bus.getExtension(Configurer.class)).thenReturn(original);
        new CxfCertificateValidationDisabler().initialize(bus);

        ArgumentCaptor<Configurer> configurer = ArgumentCaptor.forClass(Configurer.class);
        verify(bus).setExtension(configurer.capture(), any());
        HTTPConduit conduit = mock(HTTPConduit.class);
        configurer.getValue().configureBean("test", conduit);

        verify(original).configureBean("test", conduit);
        verify(conduit).setTlsClientParameters(any());
        verify(bus, never()).setProperty(anyString(), any());
    }

    @Test
    public void endrer_ikke_globale_tls_innstillinger() throws Exception {
        SSLContext defaultContext = SSLContext.getDefault();
        SSLSocketFactory defaultFactory = HttpsURLConnection.getDefaultSSLSocketFactory();
        HostnameVerifier defaultVerifier = HttpsURLConnection.getDefaultHostnameVerifier();
        String provider = Security.getProperty("ssl.SocketFactory.provider");
        Properties properties = new Properties();
        properties.putAll(System.getProperties());

        SSLTestUtils.createSSLContext();
        new TrustAllX509TrustManager();
        new TrustAllSSLSocketFactory();
        new CxfCertificateValidationDisabler().initialize(mock(Bus.class));
        assertThrows(UnsupportedOperationException.class, SSLTestUtils::disableCertificateChecks);
        assertThrows(UnsupportedOperationException.class, SSLTestUtils::setupKeyAndTrustStore);
        assertThrows(UnsupportedOperationException.class, CxfCertificateValidationDisabler::init);
        assertThrows(
                UnsupportedOperationException.class, () -> new CertificateCheckDisabler().contextInitialized(null));

        assertSame(defaultContext, SSLContext.getDefault());
        assertSame(defaultFactory, HttpsURLConnection.getDefaultSSLSocketFactory());
        assertSame(defaultVerifier, HttpsURLConnection.getDefaultHostnameVerifier());
        assertEquals(provider, Security.getProperty("ssl.SocketFactory.provider"));
        assertEquals(properties, System.getProperties());
    }

    private void startServer(String resource) throws Exception {
        KeyManagerFactory keys = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
        keys.init(keyStore(resource), "password".toCharArray());
        SSLContext context = SSLContext.getInstance("TLS");
        context.init(keys.getKeyManagers(), null, null);
        server = HttpsServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.setHttpsConfigurator(new HttpsConfigurator(context));
        server.createContext("/", exchange -> {
            try {
                exchange.sendResponseHeaders(200, -1);
            } finally {
                exchange.close();
            }
        });
        server.start();
    }

    private int status(String hostname, SSLSocketFactory factory) throws Exception {
        HttpsURLConnection connection = (HttpsURLConnection)
                new URL("https://" + hostname + ":" + server.getAddress().getPort() + "/").openConnection();
        connection.setSSLSocketFactory(factory);
        connection.setConnectTimeout(5000);
        connection.setReadTimeout(5000);
        try {
            return connection.getResponseCode();
        } finally {
            connection.disconnect();
        }
    }

    private X509Certificate[] certificateChain(String resource) throws Exception {
        return new X509Certificate[] {(X509Certificate) keyStore(resource).getCertificate("selfsigned")};
    }

    private KeyStore keyStore(String resource) throws Exception {
        try (InputStream input = SSLTestUtilsTest.class.getResourceAsStream(resource)) {
            assertNotNull(input);
            KeyStore store = KeyStore.getInstance("JKS");
            store.load(input, "password".toCharArray());
            return store;
        }
    }
}
