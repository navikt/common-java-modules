package no.nav.fo.apiapp;

import no.nav.apiapp.ApiApp;
import no.nav.apiapp.ApiApplication;
import no.nav.dialogarena.config.fasit.FasitUtils;
import no.nav.dialogarena.config.fasit.ServiceUser;
import no.nav.dialogarena.config.fasit.dto.RestService;
import no.nav.json.JsonProvider;
import no.nav.sbl.dialogarena.common.abac.pep.CredentialConstants;
import no.nav.sbl.dialogarena.common.abac.pep.service.AbacServiceConfig;
import no.nav.sbl.dialogarena.common.cxf.StsSecurityConstants;
import no.nav.sbl.dialogarena.common.jetty.Jetty;
import no.nav.testconfig.ApiAppTest;
import org.eclipse.jetty.server.ServerConnector;
import org.glassfish.jersey.client.ClientProperties;
import org.junit.BeforeClass;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.context.WebApplicationContext;

import javax.ws.rs.client.*;
import javax.ws.rs.core.NewCookie;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriBuilder;
import java.io.IOException;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.URI;
import java.net.UnknownHostException;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

import static javax.ws.rs.core.MediaType.APPLICATION_JSON_TYPE;
import static no.nav.apiapp.ServletUtil.getContext;
import static no.nav.brukerdialog.security.oidc.provider.AzureADB2CConfig.AZUREAD_B2C_DISCOVERY_URL_PROPERTY_NAME;
import static no.nav.brukerdialog.security.oidc.provider.AzureADB2CConfig.AZUREAD_B2C_EXPECTED_AUDIENCE_PROPERTY_NAME;
import static no.nav.common.auth.openam.sbs.OpenAmConfig.OPENAM_RESTURL;
import static no.nav.dialogarena.config.fasit.FasitUtils.Zone.FSS;
import static no.nav.dialogarena.config.util.Util.setProperty;


public abstract class JettyTest {

    public static boolean DISABLE_AUTH = false;
    public static final String APPLICATION_NAME = "api-app";

    static {
        ApiAppTest.setupTestContext(ApiAppTest.Config.builder()
                .applicationName(APPLICATION_NAME)
                .build()
        );
        setupContext();
    }

    private static final Logger LOGGER = LoggerFactory.getLogger(JettyTest.class);

    private static Jetty JETTY;

    public static Jetty nyJettyForTest(Class<? extends ApiApplication> apiAppClass) {
        DISABLE_AUTH = true;
        ApiApp apiApp = ApiApp.startApiApp(apiAppClass, new String[]{Integer.toString(tilfeldigPort()), Integer.toString(tilfeldigPort())});
        return apiApp.getJetty();
    }

    private Client client = ClientBuilder.newBuilder()
            .register(new JsonProvider())
            .property(ClientProperties.FOLLOW_REDIRECTS, "false")
            .build();

    private Map<String, NewCookie> cookies = new HashMap<>();

    @BeforeClass
    public static void startJetty() {
        if (JETTY == null) {
            JETTY = nyJettyForTest(ApplicationConfig.class);
            Runtime.getRuntime().addShutdownHook(new Thread(JETTY.stop::run));
        }
    }

    public static int tilfeldigPort() {
        try (ServerSocket serverSocket = new ServerSocket(0)) {
            return serverSocket.getLocalPort();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    protected Response get(String path) {
        return request(path, SyncInvoker::get);
    }

    protected Response request(String path, Function<Invocation.Builder, Response> ex) {
        URI uri = uri(path);
        LOGGER.info("request: {}", uri);
        Invocation.Builder request = client.target(uri).request();
        cookies.forEach((k, v) -> request.cookie(k, v.getValue()));
        Response response = ex.apply(request);
        response.getCookies().forEach((k, v) -> cookies.put(k, v));
        LOGGER.info("[response] status={} cookies={}", response.getStatus(), cookies);
        return response;
    }

    protected WebTarget target(String path) {
        URI uri = uri(path);
        return client.target(uri);
    }

    protected static URI uri(String path) {
        return buildUri(path).build();
    }

    protected static UriBuilder buildUri(String path) {
        return UriBuilder.fromPath(APPLICATION_NAME + path).host(getHostName()).scheme("https").port(getSslPort());
    }

    protected String getString(String path) {
        return get(path).readEntity(String.class);
    }

    protected String putJson(String path, String jsonPayload) {
        return put(path, Entity.entity(jsonPayload, APPLICATION_JSON_TYPE)).readEntity(String.class);
    }

    protected Response put(String path, Entity<String> entity) {
        return request(path, r -> r.put(entity));
    }

    protected String postJson(String path, String jsonPayload) {
        return request(path, r -> r.post(Entity.entity(jsonPayload, APPLICATION_JSON_TYPE))).readEntity(String.class);
    }

    protected <T> T getBean(Class<T> aClass) {
        WebApplicationContext webApplicationContext = getContext(JETTY.context.getServletContext());
        return webApplicationContext.getBean(aClass);
    }

    protected Map<String, NewCookie> getCookies() {
        return cookies;
    }

    protected static int getSslPort() {
        return getSslPort(JETTY);
    }

    public static int getSslPort(Jetty jetty) {
        return ((ServerConnector) jetty.server.getConnectors()[1]).getPort();
    }

    public static String getHostName() {
        try {
            return InetAddress.getLocalHost().getCanonicalHostName();
        } catch (UnknownHostException e) {
            throw new RuntimeException(e);
        }
    }

    protected static void setupContext() {
        String securityTokenService = FasitUtils.getBaseUrl("securityTokenService", FSS);
        ServiceUser srvveilarbdemo = FasitUtils.getServiceUser("srvveilarbdemo", "veilarbdemo");
        RestService abacEndpoint = FasitUtils.getRestService("abac.pdp.endpoint", srvveilarbdemo.getEnvironment());

        setProperty(StsSecurityConstants.STS_URL_KEY, securityTokenService);
        setProperty(StsSecurityConstants.SYSTEMUSER_USERNAME, srvveilarbdemo.getUsername());
        setProperty(StsSecurityConstants.SYSTEMUSER_PASSWORD, srvveilarbdemo.getPassword());

        setProperty(OPENAM_RESTURL, "https://itjenester-" + FasitUtils.getDefaultTestEnvironment().toString() + ".oera.no/esso");

        ServiceUser azureADClientId = FasitUtils.getServiceUser("aad_b2c_clientid", "veilarbdemo");
        setProperty(AZUREAD_B2C_DISCOVERY_URL_PROPERTY_NAME, FasitUtils.getBaseUrl("aad_b2c_discovery"));
        setProperty(AZUREAD_B2C_EXPECTED_AUDIENCE_PROPERTY_NAME, azureADClientId.username);

        setProperty(CredentialConstants.SYSTEMUSER_USERNAME, srvveilarbdemo.getUsername());
        setProperty(CredentialConstants.SYSTEMUSER_PASSWORD, srvveilarbdemo.getPassword());
        setProperty(AbacServiceConfig.ABAC_ENDPOINT_URL_PROPERTY_NAME, abacEndpoint.getUrl());
    }

}
