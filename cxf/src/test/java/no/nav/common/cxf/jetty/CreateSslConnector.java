package no.nav.common.cxf.jetty;

import no.nav.common.utils.EnvironmentUtils;
import org.eclipse.jetty.http.HttpVersion;
import org.eclipse.jetty.server.*;
import org.eclipse.jetty.util.ssl.SslContextFactory;

import java.util.function.Function;

class CreateSslConnector implements Function<Integer, ServerConnector> {

    private final Server jetty;
    private final HttpConfiguration baseConfiguration;

    CreateSslConnector(Server jetty, HttpConfiguration baseConfiguration) {
        this.jetty = jetty;
        this.baseConfiguration = baseConfiguration;
    }

    @Override
    public ServerConnector apply(Integer sslPort) {

        SslContextFactory factory = new SslContextFactory(true);
        factory.setKeyStorePath(EnvironmentUtils.getRequiredProperty("javax.net.ssl.trustStore"));
        factory.setKeyStorePassword(EnvironmentUtils.getRequiredProperty("javax.net.ssl.trustStorePassword"));

        HttpConfiguration httpsConfiguration = new HttpConfiguration(baseConfiguration);
        httpsConfiguration.setSecureScheme("https");
        httpsConfiguration.setSecurePort(sslPort);
        httpsConfiguration.addCustomizer(new SecureRequestCustomizer());

        ServerConnector sslConnector = new ServerConnector(jetty,
                new SslConnectionFactory(factory, HttpVersion.HTTP_1_1.toString()),
                new HttpConnectionFactory(httpsConfiguration));
        sslConnector.setPort(sslPort);
        return sslConnector;
    }

}