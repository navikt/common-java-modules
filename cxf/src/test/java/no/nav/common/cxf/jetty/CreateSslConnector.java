package no.nav.common.cxf.jetty;

import java.util.function.Function;
import no.nav.common.test.ssl.SSLTestUtils;
import org.eclipse.jetty.http.HttpVersion;
import org.eclipse.jetty.server.HttpConfiguration;
import org.eclipse.jetty.server.HttpConnectionFactory;
import org.eclipse.jetty.server.SecureRequestCustomizer;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.ServerConnector;
import org.eclipse.jetty.server.SslConnectionFactory;
import org.eclipse.jetty.util.ssl.SslContextFactory;

class CreateSslConnector implements Function<Integer, ServerConnector> {

    private final Server jetty;
    private final HttpConfiguration baseConfiguration;

    CreateSslConnector(Server jetty, HttpConfiguration baseConfiguration) {
        this.jetty = jetty;
        this.baseConfiguration = baseConfiguration;
    }

    @Override
    public ServerConnector apply(Integer sslPort) {

        SslContextFactory.Server factory = new SslContextFactory.Server();
        factory.setKeyStorePath(SSLTestUtils.class.getResource("/dummy.jks").toExternalForm());
        factory.setKeyStorePassword("password");

        HttpConfiguration httpsConfiguration = new HttpConfiguration(baseConfiguration);
        httpsConfiguration.setSecureScheme("https");
        httpsConfiguration.setSecurePort(sslPort);
        httpsConfiguration.addCustomizer(new SecureRequestCustomizer());

        ServerConnector sslConnector = new ServerConnector(
                jetty,
                new SslConnectionFactory(factory, HttpVersion.HTTP_1_1.toString()),
                new HttpConnectionFactory(httpsConfiguration));
        sslConnector.setPort(sslPort);
        return sslConnector;
    }
}
