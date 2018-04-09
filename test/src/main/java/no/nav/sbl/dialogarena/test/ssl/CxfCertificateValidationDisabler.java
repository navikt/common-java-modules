package no.nav.sbl.dialogarena.test.ssl;

import lombok.SneakyThrows;
import org.apache.commons.io.IOUtils;
import org.apache.cxf.Bus;
import org.apache.cxf.configuration.Configurer;
import org.apache.cxf.configuration.jsse.TLSClientParameters;
import org.apache.cxf.endpoint.Client;
import org.apache.cxf.endpoint.Server;
import org.apache.cxf.feature.Feature;
import org.apache.cxf.interceptor.InterceptorProvider;
import org.apache.cxf.transport.http.HTTPConduit;
import org.apache.cxf.transport.http.URLConnectionHTTPConduit;
import org.apache.wss4j.dom.validate.Validator;

import java.io.File;
import java.io.FileOutputStream;

import static java.lang.System.setProperty;
import static java.util.Optional.ofNullable;
import static no.nav.sbl.dialogarena.test.ssl.SSLTestUtils.ALLOW_ALL_HOSTNAME_VERIFIER;
import static no.nav.sbl.dialogarena.test.ssl.SSLTestUtils.TRUST_ALL_SSL_SOCKET_FACTORY;
import static org.apache.cxf.ws.security.SecurityConstants.SAML2_TOKEN_VALIDATOR;

public class CxfCertificateValidationDisabler implements Feature {

    private static final Validator ALLOW_ALL_VALIDATOR = (Validator) (credential, data) -> credential;

    @SneakyThrows
    static void init() {
        File target = new File("target");
        target.mkdirs();
        File cxfFile = new File(target, "cxf.xml");
        try (FileOutputStream output = new FileOutputStream(cxfFile)) {
            IOUtils.copy(CxfCertificateValidationDisabler.class.getResourceAsStream("/cxf.xml"), output);
        }
        String cxfFileAbsolutePath = cxfFile.getAbsolutePath();
        setProperty("cxf.config.file", cxfFileAbsolutePath);
    }

    @Override
    public void initialize(Server server, Bus bus) {}

    @Override
    public void initialize(Client client, Bus bus) {}

    @Override
    public void initialize(InterceptorProvider interceptorProvider, Bus bus) {}

    @Override
    public void initialize(Bus bus) {
        bus.setExtension(new HttpConduitConfigurer(bus.getExtension(Configurer.class)), Configurer.class);
        bus.setProperty(SAML2_TOKEN_VALIDATOR, ALLOW_ALL_VALIDATOR);
    }

    private class HttpConduitConfigurer implements Configurer {
        private final Configurer originalConfigurer;

        private HttpConduitConfigurer(Configurer originalConfigurer) {
            this.originalConfigurer = originalConfigurer;
        }

        @Override
        public void configureBean(Object beanInstance) {
            configureHttpConduit(beanInstance);
            originalConfigurer.configureBean(beanInstance);
        }

        @Override
        public void configureBean(String name, Object beanInstance) {
            configureHttpConduit(beanInstance);
            originalConfigurer.configureBean(name, beanInstance);
        }

        private void configureHttpConduit(Object beanInstance) {
            if(beanInstance instanceof HTTPConduit){
                HTTPConduit httpConduit = (URLConnectionHTTPConduit) beanInstance;
                TLSClientParameters tlsClientParameters = ofNullable(httpConduit.getTlsClientParameters()).orElseGet(TLSClientParameters::new);
                tlsClientParameters.setDisableCNCheck(true);
                tlsClientParameters.setHostnameVerifier(ALLOW_ALL_HOSTNAME_VERIFIER);
                tlsClientParameters.setSSLSocketFactory(TRUST_ALL_SSL_SOCKET_FACTORY);
                httpConduit.setTlsClientParameters(tlsClientParameters);
            }
        }
    }
}
