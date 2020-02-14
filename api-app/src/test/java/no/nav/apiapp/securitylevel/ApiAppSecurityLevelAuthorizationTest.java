package no.nav.apiapp.securitylevel;

import no.nav.common.auth.SecurityLevel;
import no.nav.sbl.dialogarena.common.jetty.Jetty;
import no.nav.sbl.dialogarena.test.junit.SystemPropertiesRule;
import no.nav.sbl.rest.RestUtils;
import no.nav.testconfig.security.JwtTestTokenIssuer;
import no.nav.testconfig.security.OidcProviderTestRule;
import org.eclipse.jetty.http.HttpStatus;
import org.junit.*;

import javax.ws.rs.client.Client;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriBuilder;

import static javax.ws.rs.core.HttpHeaders.USER_AGENT;
import static no.nav.brukerdialog.security.oidc.OidcTokenUtils.SECURITY_LEVEL_ATTRIBUTE;
import static no.nav.brukerdialog.security.oidc.provider.AzureADB2CConfig.EXTERNAL_USERS_AZUREAD_B2C_DISCOVERY_URL;
import static no.nav.brukerdialog.security.oidc.provider.AzureADB2CConfig.EXTERNAL_USERS_AZUREAD_B2C_EXPECTED_AUDIENCE;
import static no.nav.common.auth.SecurityLevel.*;
import static no.nav.common.auth.SecurityLevel.Level2;
import static no.nav.fo.apiapp.rest.JettyTestUtils.*;
import static no.nav.log.LogFilter.CONSUMER_ID_HEADER_NAME;
import static no.nav.log.LogFilter.PREFERRED_NAV_CALL_ID_HEADER_NAME;
import static org.assertj.core.api.Assertions.assertThat;

public class ApiAppSecurityLevelAuthorizationTest {


    @ClassRule
    public static OidcProviderTestRule oidcProviderTestRule = new OidcProviderTestRule(tilfeldigPort());

    @Rule
    public SystemPropertiesRule systemPropertiesRule = new SystemPropertiesRule();

    private static Jetty jetty;
    private Client client;
    private UriBuilder uriBuilder;

    @Before
    public void start() {
        setupContext();
        systemPropertiesRule.setProperty(EXTERNAL_USERS_AZUREAD_B2C_DISCOVERY_URL, oidcProviderTestRule.getDiscoveryUri());
        systemPropertiesRule.setProperty(EXTERNAL_USERS_AZUREAD_B2C_EXPECTED_AUDIENCE, oidcProviderTestRule.getAudience());
        jetty = nyJettyForTest(ApplicationConfigWithSecurityLevels.class);
        client = RestUtils.createClient();
        uriBuilder = uriBuilder("/" + APPLICATION_NAME + "/api", jetty);
    }

    @After
    public void stopJetty() {
        jetty.stop.run();
    }


    @Test
    public void api_app_security_level_authorization() {

        assertThat(get("/default", Level4).getStatus()).isEqualTo(HttpStatus.OK_200);
        assertThat(get("/default?foo=123&bar=321", Level4).getStatus()).isEqualTo(HttpStatus.OK_200);
        assertThat(get("/default/path", Level4).getStatus()).isEqualTo(HttpStatus.OK_200);
        assertThat(get("/default/path?foo=123&bar=321", Level4).getStatus()).isEqualTo(HttpStatus.OK_200);
        assertThat(get("/level4", Level4).getStatus()).isEqualTo(HttpStatus.OK_200);
        assertThat(get("/level4?foo=123&bar=321", Level4).getStatus()).isEqualTo(HttpStatus.OK_200);
        assertThat(get("/level4/path", Level4).getStatus()).isEqualTo(HttpStatus.OK_200);
        assertThat(get("/level4/path?foo=123&bar=321", Level4).getStatus()).isEqualTo(HttpStatus.OK_200);
        assertThat(get("/level4/with/long/path", Level4).getStatus()).isEqualTo(HttpStatus.OK_200);
        assertThat(get("/level4/with/long/path?foo=123&bar=321", Level4).getStatus()).isEqualTo(HttpStatus.OK_200);
        assertThat(get("/level2", Level4).getStatus()).isEqualTo(HttpStatus.OK_200);
        assertThat(get("/level2?foo=123&bar=321", Level4).getStatus()).isEqualTo(HttpStatus.OK_200);
        assertThat(get("/level2/path", Level4).getStatus()).isEqualTo(HttpStatus.OK_200);
        assertThat(get("/level2/path?foo=123&bar=321", Level4).getStatus()).isEqualTo(HttpStatus.OK_200);
        assertThat(get("/level2/with/long/path", Level4).getStatus()).isEqualTo(HttpStatus.OK_200);
        assertThat(get("/level2/with/long/path?foo=123&bar=321", Level4).getStatus()).isEqualTo(HttpStatus.OK_200);

        assertThat(get("/default", Level3).getStatus()).isEqualTo(HttpStatus.OK_200);
        assertThat(get("/default/path", Level3).getStatus()).isEqualTo(HttpStatus.OK_200);
        assertThat(get("/level4", Level3).getStatus()).isEqualTo(HttpStatus.UNAUTHORIZED_401);
        assertThat(get("/level4/path", Level3).getStatus()).isEqualTo(HttpStatus.UNAUTHORIZED_401);
        assertThat(get("/level4/with/long/path", Level3).getStatus()).isEqualTo(HttpStatus.UNAUTHORIZED_401);
        assertThat(get("/level2", Level3).getStatus()).isEqualTo(HttpStatus.OK_200);
        assertThat(get("/level2/path", Level3).getStatus()).isEqualTo(HttpStatus.OK_200);
        assertThat(get("/level2/with/long/path", Level3).getStatus()).isEqualTo(HttpStatus.OK_200);

        assertThat(get("/default", Level2).getStatus()).isEqualTo(HttpStatus.UNAUTHORIZED_401);
        assertThat(get("/default/path", Level2).getStatus()).isEqualTo(HttpStatus.UNAUTHORIZED_401);
        assertThat(get("/level4", Level2).getStatus()).isEqualTo(HttpStatus.UNAUTHORIZED_401);
        assertThat(get("/level4/path", Level2).getStatus()).isEqualTo(HttpStatus.UNAUTHORIZED_401);
        assertThat(get("/level4/with/long/path", Level2).getStatus()).isEqualTo(HttpStatus.UNAUTHORIZED_401);
        assertThat(get("/level2", Level2).getStatus()).isEqualTo(HttpStatus.OK_200);
        assertThat(get("/level2/path", Level2).getStatus()).isEqualTo(HttpStatus.OK_200);
        assertThat(get("/level2/with/long/path", Level2).getStatus()).isEqualTo(HttpStatus.OK_200);

        assertThat(get("/default", Level1).getStatus()).isEqualTo(HttpStatus.UNAUTHORIZED_401);
        assertThat(get("/default/path", Level1).getStatus()).isEqualTo(HttpStatus.UNAUTHORIZED_401);
        assertThat(get("/level4", Level1).getStatus()).isEqualTo(HttpStatus.UNAUTHORIZED_401);
        assertThat(get("/level4/path", Level1).getStatus()).isEqualTo(HttpStatus.UNAUTHORIZED_401);
        assertThat(get("/level4/with/long/path", Level1).getStatus()).isEqualTo(HttpStatus.UNAUTHORIZED_401);
        assertThat(get("/level2", Level1).getStatus()).isEqualTo(HttpStatus.UNAUTHORIZED_401);
        assertThat(get("/level2/path", Level1).getStatus()).isEqualTo(HttpStatus.UNAUTHORIZED_401);
        assertThat(get("/level2/with/long/path", Level1).getStatus()).isEqualTo(HttpStatus.UNAUTHORIZED_401);

        assertThat(get("/default", Ukjent).getStatus()).isEqualTo(HttpStatus.UNAUTHORIZED_401);
        assertThat(get("/default/path", Ukjent).getStatus()).isEqualTo(HttpStatus.UNAUTHORIZED_401);
        assertThat(get("/level4", Ukjent).getStatus()).isEqualTo(HttpStatus.UNAUTHORIZED_401);
        assertThat(get("/level4/path", Ukjent).getStatus()).isEqualTo(HttpStatus.UNAUTHORIZED_401);
        assertThat(get("/level4/with/long/path", Ukjent).getStatus()).isEqualTo(HttpStatus.UNAUTHORIZED_401);
        assertThat(get("/level2", Ukjent).getStatus()).isEqualTo(HttpStatus.UNAUTHORIZED_401);
        assertThat(get("/level2/path", Ukjent).getStatus()).isEqualTo(HttpStatus.UNAUTHORIZED_401);
        assertThat(get("/level2/with/long/path", Ukjent).getStatus()).isEqualTo(HttpStatus.UNAUTHORIZED_401);
    }

    private Response get(String path, SecurityLevel securityLevel) {
        return client.target(uriBuilder.toString() + path)
                .request()
                .header(USER_AGENT, "mozilla chrome IE etc")
                .header("Authorization", "Bearer " + oidcProviderTestRule.getToken(
                        new JwtTestTokenIssuer
                                .Claims("0")
                                .setClaim(SECURITY_LEVEL_ATTRIBUTE, securityLevel)))
                .get();
    }
}
