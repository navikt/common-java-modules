package no.nav.common.client.axsys;

import com.github.tomakehurst.wiremock.junit.WireMockRule;
import no.nav.common.client.TestUtils;
import no.nav.common.test.junit.SystemPropertiesRule;
import no.nav.common.json.JsonUtils;
import no.nav.common.types.identer.EnhetId;
import no.nav.common.types.identer.NavIdent;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.slf4j.MDC;

import javax.ws.rs.core.HttpHeaders;
import java.util.List;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static javax.ws.rs.core.HttpHeaders.AUTHORIZATION;
import static no.nav.common.log.LogFilter.CONSUMER_ID_HEADER_NAME;
import static no.nav.common.log.LogFilter.PREFERRED_NAV_CALL_ID_HEADER_NAME;
import static no.nav.common.log.MDCConstants.MDC_CALL_ID;
import static no.nav.common.rest.client.RestUtils.MEDIA_TYPE_JSON;
import static no.nav.common.utils.EnvironmentUtils.NAIS_APP_NAME_PROPERTY_NAME;
import static no.nav.common.utils.IdUtils.generateId;
import static org.junit.Assert.assertEquals;

public class AxsysClientTest {

    private static final String TEST_RESOURCE_BASE_PATH = "no/nav/common/client/axsys/";
    private static final String APPLICATION_NAME = "test_app";

    @Rule
    public WireMockRule wireMockRule = new WireMockRule(0);
    @Rule
    public SystemPropertiesRule systemPropertiesRule = new SystemPropertiesRule();

    @Before
    public void setup() {
        systemPropertiesRule.setProperty(NAIS_APP_NAME_PROPERTY_NAME, APPLICATION_NAME);
    }

    @Test
    public void hentAnsatte__skal_hente_ansatte() {
        String jobId = generateId();
        MDC.put(MDC_CALL_ID, jobId);

        String json = TestUtils.readTestResourceFile(TEST_RESOURCE_BASE_PATH + "ansatte.json");
        List<AxsysClientImpl.AxsysEnhetBruker> jsonEnhet = JsonUtils.fromJsonArray(json, AxsysClientImpl.AxsysEnhetBruker.class);
        List<NavIdent> brukere = jsonEnhet.stream().map(AxsysClientImpl.AxsysEnhetBruker::getAppIdent).collect(Collectors.toList());
        String baseUrl = "http://localhost:" + wireMockRule.port();

        givenThat(get("/api/v1/enhet/1234/brukere")
                .withHeader(HttpHeaders.ACCEPT, equalTo(MEDIA_TYPE_JSON.toString()))
                .withHeader(CONSUMER_ID_HEADER_NAME, equalTo(APPLICATION_NAME))
                .withHeader(PREFERRED_NAV_CALL_ID_HEADER_NAME, equalTo(jobId))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withBody(json))
        );

        AxsysClient client = new AxsysClientImpl(baseUrl);
        assertEquals(client.hentAnsatte(new EnhetId("1234")), brukere);

        MDC.remove(MDC_CALL_ID);
    }


    @Test
    public void hentTilganger__skal_hente_tilganger() {
        String jobId = generateId();
        MDC.put(MDC_CALL_ID, jobId);
        String json = TestUtils.readTestResourceFile(TEST_RESOURCE_BASE_PATH + "tilganger.json");
        List<AxsysEnhet> jsonTilganger = JsonUtils.fromJson(json, AxsysClientImpl.AxsysEnheter.class).getEnheter();
        String baseUrl = "http://localhost:" + wireMockRule.port();

        givenThat(get("/api/v1/tilgang/Z123456")
                .withHeader(HttpHeaders.ACCEPT, equalTo(MEDIA_TYPE_JSON.toString()))
                .withHeader(CONSUMER_ID_HEADER_NAME, equalTo(APPLICATION_NAME))
                .withHeader(PREFERRED_NAV_CALL_ID_HEADER_NAME, equalTo(jobId))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withBody(json))
        );

        AxsysClient client = new AxsysClientImpl(baseUrl);
        assertEquals(client.hentTilganger(new NavIdent("Z123456")), jsonTilganger);

        MDC.remove(MDC_CALL_ID);
    }

    @Test
    public void axsysClientV2_sender_med_authorization_header() {
        String jobId = generateId();
        MDC.put(MDC_CALL_ID, jobId);
        String json = TestUtils.readTestResourceFile(TEST_RESOURCE_BASE_PATH + "tilganger.json");
        List<AxsysEnhet> jsonTilganger = JsonUtils.fromJson(json, AxsysClientImpl.AxsysEnheter.class).getEnheter();
        String baseUrl = "http://localhost:" + wireMockRule.port();
        Supplier<String> tokenSupplier = () -> "mitt-token";

        givenThat(get("/api/v2/tilgang/Z123456")
                .withHeader(HttpHeaders.ACCEPT, equalTo(MEDIA_TYPE_JSON.toString()))
                .withHeader(CONSUMER_ID_HEADER_NAME, equalTo(APPLICATION_NAME))
                .withHeader(PREFERRED_NAV_CALL_ID_HEADER_NAME, equalTo(jobId))
                .withHeader(AUTHORIZATION, equalTo("Bearer mitt-token"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withBody(json))
        );

        AxsysClient client = new AxsysV2ClientImpl(baseUrl, tokenSupplier);
        assertEquals(client.hentTilganger(new NavIdent("Z123456")), jsonTilganger);

        MDC.remove(MDC_CALL_ID);
    }
}
