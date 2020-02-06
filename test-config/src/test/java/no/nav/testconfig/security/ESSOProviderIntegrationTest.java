package no.nav.testconfig.security;

import no.nav.common.auth.Subject;
import no.nav.common.auth.openam.sbs.OpenAMUserInfoService;
import no.nav.fasit.FasitUtils;
import no.nav.fasit.TestEnvironment;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.core.Response;
import java.io.IOException;
import java.net.HttpCookie;
import java.net.URI;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import static no.nav.common.auth.openam.sbs.OpenAMLoginFilter.NAV_ESSO_COOKIE_NAVN;
import static no.nav.common.auth.openam.sbs.OpenAMUserInfoService.PARAMETER_SECURITY_LEVEL;
import static no.nav.common.auth.openam.sbs.OpenAMUserInfoService.PARAMETER_UID;
import static no.nav.fasit.TestEnvironment.Q0;
import static no.nav.fasit.TestEnvironment.Q6;
import static no.nav.sbl.rest.RestUtils.withClient;
import static no.nav.testconfig.security.ESSOProvider.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assume.assumeFalse;
import static org.junit.Assume.assumeTrue;


public class ESSOProviderIntegrationTest {

    private static final Logger LOGGER = LoggerFactory.getLogger(ESSOProviderIntegrationTest.class);

    @Test
    public void skal_hente_cookie_fra_default_environment() {
        assumeAlive(FasitUtils.getDefaultTestEnvironment());
        sjekkCookie(ESSOProvider.getHttpCookie());
    }

    @Test
    public void skal_hente_cookie_fra_q6() {
        assumeAlive(Q6);
        sjekkCookie(ESSOProvider.getHttpCookie(Q6));
    }

    @Test
    public void skal_hente_cookie_fra_privat_bruker_i_q6() {
        assumeAlive(Q6);
        sjekkCookie(ESSOProvider.getHttpCookie(Q6, PRIVAT_BRUKER));
    }

    @Test
    public void skal_hente_cookie_fra_q0() {
        assumeAlive(Q0);
        sjekkCookie(ESSOProvider.getHttpCookie(Q0));
    }

    @Test
    public void getEssoCredentialsForUser_q6() {
        assumeAlive(Q6);
        sjekkCredentials(ESSOProvider.getEssoCredentialsForUser(BRUKER_UNDER_OPPFOLGING, Q6));
    }

    @Test
    public void getUserInfo() throws IOException {
        assumeAlive(FasitUtils.getDefaultTestEnvironment());

        String token = ESSOProvider.getHttpCookie().getValue();
        String endpoint = FasitUtils.getOpenAmConfig().getRestUrl();

        OpenAMUserInfoService openAMUserInfoService = new OpenAMUserInfoService(URI.create(endpoint));
        Subject subject = openAMUserInfoService.convertTokenToSubject(token).get();
        assertThat(subject.getUid()).isNotNull().isNotEmpty();

        List<String> attributes = Arrays.asList(PARAMETER_UID, PARAMETER_SECURITY_LEVEL);
        Map<String, String> stringStringMap = openAMUserInfoService.getUserInfo(token, attributes).get();
        assertThat(stringStringMap).containsKeys(attributes.toArray(new String[0]));
    }

    private void assumeAlive(TestEnvironment testEnvironment) {
        assumeTrue(isAlive(testEnvironment));
        assumeFalse(FasitUtils.usingMock());
    }

    private Boolean isAlive(TestEnvironment testEnvironment) {
        return withClient(client -> {
            try {
                Response response = client.target(essoBaseUrl(testEnvironment.toString())).request().get();
                int status = response.getStatus();
                String loginUrl = response.getLocation().toString().toLowerCase();
                return status == 302 && loginUrl.contains("login"); // expecting redirect to login page
            } catch (Exception e) {
                LOGGER.warn(e.getMessage(), e);
                return false;
            }
        });
    }

    private void sjekkCredentials(ESSOProvider.ESSOCredentials essoCredentialsForUser) {
        sjekkCookie(essoCredentialsForUser.cookie);
    }

    private void sjekkCookie(HttpCookie httpCookie) {
        assertThat(httpCookie.getName()).isEqualTo(NAV_ESSO_COOKIE_NAVN);
        assertThat(httpCookie.getValue()).isNotNull().isNotEmpty();
    }

}