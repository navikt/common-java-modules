package no.nav.common.auth.test_provider;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.jakarta.rs.json.JacksonXmlBindJsonProvider;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import lombok.SneakyThrows;
import lombok.Value;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;
import org.glassfish.jersey.server.ContainerRequest;
import org.glassfish.jersey.server.ResourceConfig;
import org.glassfish.jersey.servlet.ServletContainer;
import org.junit.rules.ExternalResource;

import java.util.HashMap;
import java.util.Map;

public class OidcProviderTestRule extends ExternalResource {

    private static final Map<Integer, JwtTestTokenIssuerConfig> issuerConfigMap = new HashMap<>();

    private static final Map<Integer, JwtTestTokenIssuer> issuerMap = new HashMap<>();

    private static final ObjectMapper jsonMapper = new ObjectMapper();

    private final int port;

    private Server httpServer;

    private final JwtTestTokenIssuerConfig issuerConfig;

    private final JwtTestTokenIssuer issuer;

    public OidcProviderTestRule(JwtTestTokenIssuerConfig issuerConfig) {
        this.port = PortUtils.findAvailablePort();
        this.issuerConfig = issuerConfig;
        this.issuer = JwtTestTokenUtil.testTokenIssuer(issuerConfig);
    }

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
        issuerConfigMap.put(port, issuerConfig);
        issuerMap.put(port, issuer);

        ServletContextHandler contextHandler = new ServletContextHandler();
        contextHandler.setContextPath("/");

        httpServer = new Server(port);
        httpServer.setHandler(contextHandler);

        ServletHolder servletHolder =
                new ServletHolder(
                        new ServletContainer(
                                new ResourceConfig(OidcProviderServlet.class, JacksonXmlBindJsonProvider.class)));

        servletHolder.setInitOrder(0);
        contextHandler.addServlet(servletHolder, "/*");

        httpServer.start();
    }


    private void stopServer() throws Exception {
        issuerConfigMap.remove(port);
        issuerMap.remove(port);

        if (httpServer != null) {
            httpServer.stop();
        }
    }


    public String getToken(JwtTestTokenIssuer.Claims claims) {
        return issuer.issueTestToken(claims);
    }

    public String getAudience() {
        return issuerConfig.audience;
    }

    public String getDiscoveryUri() {
        return basePath() + "/discovery";
    }

    public String getJwksUri() {
        return basePath() + "/jwks";
    }

    public String getRefreshUri() {
        return basePath() + "/refresh";
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
            JwtTestTokenIssuerConfig config = issuerConfigMap.get(request.getBaseUri().getPort());
            return new IssuerMetaData(config.issuer, request.getBaseUri().toString() + "jwks");
        }

        @GET
        @Path("/jwks")
        @Produces(MediaType.APPLICATION_JSON)
        public String jwt(@Context ContainerRequest request) {
            JwtTestTokenIssuer issuer = issuerMap.get(request.getBaseUri().getPort());
            return issuer.getKeySetJson();
        }

        @POST
        @Path("/refresh")
        @Produces(MediaType.APPLICATION_JSON)
        public String refresh(@Context ContainerRequest request) throws JsonProcessingException {
            JwtTestTokenIssuer issuer = issuerMap.get(request.getBaseUri().getPort());
            String idToken = issuer.issueTestToken(new JwtTestTokenIssuer.Claims("subject"));
            return jsonMapper.writeValueAsString(new RefreshResult(idToken));
        }

        @Value
        private static class IssuerMetaData {
            public String issuer;
            public String jwks_uri;
        }

        @Value
        private static class RefreshResult {
            String idToken;
        }
    }
}
