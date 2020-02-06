package no.nav.testconfig.security;

import com.fasterxml.jackson.jaxrs.json.JacksonJaxbJsonProvider;
import lombok.SneakyThrows;
import lombok.Value;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;
import org.glassfish.jersey.server.ContainerRequest;
import org.glassfish.jersey.server.ResourceConfig;
import org.glassfish.jersey.servlet.ServletContainer;
import org.junit.rules.ExternalResource;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;

public class OidcProviderTestRule extends ExternalResource {

    private final int port;
    private Server httpServer;

    public OidcProviderTestRule(int port) {
        this.port = port;
    }

    private static JwtTestTokenIssuerConfig azureConfig =
            JwtTestTokenIssuerConfig.builder()
                    .id("oidc-provider-test-rule-aadb2c")
                    .issuer("oidc-provider-test-rule-aadb2c")
                    .audience("oidc-provider-test-rule")
                    .build();


    private static JwtTestTokenIssuer testTokenIssuer = JwtTestTokenUtil.testTokenIssuer(azureConfig);



    @Override
    protected void before() throws Throwable {
        startServer();
    }

    @Override
    @SneakyThrows
    protected void after() {
        stopServer();
    }

    private void startServer() throws Exception {
        ServletContextHandler contextHandler = new ServletContextHandler();
        contextHandler.setContextPath("/");

        httpServer = new Server(port);
        httpServer.setHandler(contextHandler);

        ServletHolder servletHolder =
                new ServletHolder(
                        new ServletContainer(
                                new ResourceConfig(OidcProviderServlet.class, JacksonJaxbJsonProvider.class)));

        servletHolder.setInitOrder(0);
        contextHandler.addServlet(servletHolder, "/*");

        httpServer.start();
    }


    private void stopServer() throws Exception {
        if (httpServer != null) {
            httpServer.stop();
        }
    }


    public String getToken(JwtTestTokenIssuer.Claims claims) {
        return testTokenIssuer.issueTestToken(claims);
    }

    public String getAudience() {
        return azureConfig.audience;
    }

    public String getDiscoveryUri() {
        return basePath() + "/discovery";
    }

    private String basePath() {
        return "http://localhost:" + port;
    }


    @Path("/")
    public static class OidcProviderServlet {

        @GET
        @Path("/discovery")
        @Produces(MediaType.APPLICATION_JSON)
        public IssuerMetaData discovery(@Context ContainerRequest request) {
            return new IssuerMetaData(azureConfig.issuer, request.getBaseUri().toString() + "jwks");
        }

        @GET
        @Path("/jwks")
        @Produces(MediaType.APPLICATION_JSON)
        public String jwt() {
            return testTokenIssuer.getKeySetJson();
        }

        @Value
        private static class IssuerMetaData {
            public String issuer;
            public String jwks_uri;
        }
    }
}
