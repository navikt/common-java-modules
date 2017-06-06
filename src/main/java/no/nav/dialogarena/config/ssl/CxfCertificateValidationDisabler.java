package no.nav.dialogarena.config.ssl;

import lombok.SneakyThrows;
import org.apache.commons.io.IOUtils;
import org.apache.cxf.Bus;
import org.apache.cxf.configuration.Configurer;
import org.apache.cxf.configuration.jsse.TLSClientParameters;
import org.apache.cxf.endpoint.AbstractEndpointFactory;
import org.apache.cxf.endpoint.Client;
import org.apache.cxf.endpoint.Server;
import org.apache.cxf.feature.Feature;
import org.apache.cxf.frontend.ClientProxyFactoryBean;
import org.apache.cxf.interceptor.InterceptorProvider;
import org.apache.cxf.transport.http.HTTPConduit;
import org.apache.wss4j.dom.validate.Validator;

import java.io.File;
import java.io.FileOutputStream;

import static java.util.Optional.ofNullable;
import static no.nav.dialogarena.config.ssl.SSLTestUtils.ALLOW_ALL_HOSTNAME_VERIFIER;
import static no.nav.dialogarena.config.ssl.SSLTestUtils.TRUST_ALL_SSL_CONTEXT_FACTORY;
import static no.nav.dialogarena.config.util.Util.setProperty;
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
    public void initialize(Server server, Bus bus) {
    }

    @Override
    @SneakyThrows
    public void initialize(Client client, Bus bus) {
        HTTPConduit httpConduit = (HTTPConduit) client.getConduit();
        TLSClientParameters tlsClientParameters = ofNullable(httpConduit.getTlsClientParameters()).orElseGet(TLSClientParameters::new);
        tlsClientParameters.setHostnameVerifier(ALLOW_ALL_HOSTNAME_VERIFIER);
        tlsClientParameters.setSSLSocketFactory(TRUST_ALL_SSL_CONTEXT_FACTORY);
        httpConduit.setTlsClientParameters(tlsClientParameters);
    }

    @Override
    public void initialize(InterceptorProvider interceptorProvider, Bus bus) {
    }

    @Override
    public void initialize(Bus bus) {
        bus.setExtension(new FeaturePropagator(bus.getExtension(Configurer.class)), Configurer.class);
        bus.setProperty(SAML2_TOKEN_VALIDATOR, ALLOW_ALL_VALIDATOR);
    }

    private class FeaturePropagator implements Configurer {
        private final Configurer originalConfigurer;

        private FeaturePropagator(Configurer originalConfigurer) {
            this.originalConfigurer = originalConfigurer;
        }

        @Override
        public void configureBean(Object beanInstance) {
            propagateFeature(beanInstance);
            originalConfigurer.configureBean(beanInstance);
        }

        @Override
        public void configureBean(String name, Object beanInstance) {
            propagateFeature(beanInstance);
            originalConfigurer.configureBean(name, beanInstance);
        }

        private void propagateFeature(Object beanInstance) {
            if (beanInstance instanceof AbstractEndpointFactory) {
                ((AbstractEndpointFactory) beanInstance).getFeatures().add(CxfCertificateValidationDisabler.this);
            } else if (beanInstance instanceof ClientProxyFactoryBean) {
                ((ClientProxyFactoryBean) beanInstance).getFeatures().add(CxfCertificateValidationDisabler.this);
            }
        }
    }
}
