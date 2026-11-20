package no.nav.common.test.ssl;

import javax.net.ssl.TrustManager;
import org.apache.cxf.Bus;
import org.apache.cxf.configuration.Configurer;
import org.apache.cxf.configuration.jsse.TLSClientParameters;
import org.apache.cxf.endpoint.Client;
import org.apache.cxf.endpoint.Server;
import org.apache.cxf.feature.Feature;
import org.apache.cxf.interceptor.InterceptorProvider;
import org.apache.cxf.transport.http.HTTPConduit;

/**
 * Konfigurerer eksplisitt testtillit uten å slå av sertifikatkontroll.
 *
 * @deprecated Konfigurer TLS eksplisitt på CXF-testklienten. Klassen beholdes for kompatibilitet og omgår ikke validering.
 */
@Deprecated
public class CxfCertificateValidationDisabler implements Feature {

    static void init() {
        throw new UnsupportedOperationException("Konfigurer TLS eksplisitt på testklientens CXF-buss");
    }

    @Override
    public void initialize(Server server, Bus bus) {}

    @Override
    public void initialize(Client client, Bus bus) {
        configureHttpConduit(client.getConduit());
    }

    @Override
    public void initialize(InterceptorProvider interceptorProvider, Bus bus) {}

    @Override
    public void initialize(Bus bus) {
        bus.setExtension(new HttpConduitConfigurer(bus.getExtension(Configurer.class)), Configurer.class);
    }

    private static void configureHttpConduit(Object beanInstance) {
        if (beanInstance instanceof HTTPConduit httpConduit) {
            TLSClientParameters parameters = new TLSClientParameters();
            parameters.setTrustManagers(new TrustManager[] {SSLTestUtils.fixtureTrustManager()});
            parameters.setSSLSocketFactory(SSLTestUtils.TEST_SSL_SOCKET_FACTORY);
            parameters.setHostnameVerifier(SSLTestUtils.TEST_HOSTNAME_VERIFIER);
            httpConduit.setTlsClientParameters(parameters);
        }
    }

    private static class HttpConduitConfigurer implements Configurer {
        private final Configurer originalConfigurer;

        private HttpConduitConfigurer(Configurer originalConfigurer) {
            this.originalConfigurer = originalConfigurer;
        }

        @Override
        public void configureBean(Object beanInstance) {
            if (originalConfigurer != null) {
                originalConfigurer.configureBean(beanInstance);
            }
            configureHttpConduit(beanInstance);
        }

        @Override
        public void configureBean(String name, Object beanInstance) {
            if (originalConfigurer != null) {
                originalConfigurer.configureBean(name, beanInstance);
            }
            configureHttpConduit(beanInstance);
        }
    }
}
