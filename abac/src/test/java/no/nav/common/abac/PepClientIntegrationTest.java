package no.nav.common.abac;

import com.github.tomakehurst.wiremock.junit.WireMockRule;
import no.nav.common.abac.service.AbacService;
import no.nav.common.abac.service.AbacServiceConfig;
import no.nav.common.auth.IdentType;
import no.nav.common.auth.SsoToken;
import no.nav.common.auth.Subject;
import no.nav.common.auth.SubjectRule;
import no.nav.common.types.feil.IngenTilgang;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import java.util.HashMap;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static no.nav.common.abac.NavAttributter.RESOURCE_VEILARB_KONTOR_LAAS;
import static no.nav.common.abac.domain.ResourceType.VeilArbPerson;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

public class PepClientIntegrationTest {

    @Rule
    public SubjectRule subjectRule = new SubjectRule();

    @Rule
    public WireMockRule wireMockRule = new WireMockRule(0);

    private final String FNR = "00000000000";
    private final String ENHET = "enhet123";
    private final String PRIVELIGERT_VEILEDER = "veileder_priviligert";
    private final String LITE_PRIVELIGERT_VEILEDER = "veileder_lite_priviligert";
    private PepClient pepClient;

    @Before
    public void setup() {
        pepClient = setupPepClient();
        setupAbacResponse();
    }

    private PepClient setupPepClient() {

        String endpointUrl = "http://localhost:" + wireMockRule.port();

        AbacServiceConfig abacServiceConfig = AbacServiceConfig.builder()
                .username("username")
                .password("password")
                .endpointUrl(endpointUrl)
                .build();

        PepImpl pep = new PepImpl(new AbacService(abacServiceConfig));

        return new PepClient(pep, "veilarb", VeilArbPerson);
    }


    private void setVeileder(String uid) {
        String veilederToken = "token" + uid;
        subjectRule.setSubject(new Subject(
                uid,
                IdentType.InternBruker,
                SsoToken.oidcToken(veilederToken, new HashMap<>())
        ));
    }

    @Test
    public void sjekkTilgangTilFnr_veilederHarTilgang() {
        setVeileder(PRIVELIGERT_VEILEDER);
        pepClient.sjekkLesetilgangTilFnr(FNR);
    }

    @Test
    public void sjekkTilgangTilFnr_veilederHarIkkeTilgang() {
        setVeileder(LITE_PRIVELIGERT_VEILEDER);
        assertThatThrownBy(() -> pepClient.sjekkLesetilgangTilFnr(FNR)).isExactlyInstanceOf(IngenTilgang.class);
    }

    @Test
    public void harTilgangTilEnhet_veilederHarTilgang() {
        setVeileder(PRIVELIGERT_VEILEDER);
        assertThat(pepClient.harTilgangTilEnhet(ENHET)).isTrue();
    }

    @Test
    public void harTilgangTilEnhet_veilederHarIkkeTilgang() {
        setVeileder(LITE_PRIVELIGERT_VEILEDER);
        assertThat(pepClient.harTilgangTilEnhet(ENHET)).isFalse();
    }


    private void setupAbacResponse() {
        gittPermitResponseFnr();
        gittDenyResponseFnr();
        gittPermitResponseEnhet();
        gittDenyResponseEnhet();
    }

    private void gittPermitResponseFnr() {
        givenThat(post(urlEqualTo("/"))
                .withRequestBody(containing(PRIVELIGERT_VEILEDER))
                .withRequestBody(containing(FNR))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withBody(permitResponse)
                )
        );
    }

    private void gittDenyResponseFnr() {
        givenThat(post(urlEqualTo("/"))
                .withRequestBody(containing(LITE_PRIVELIGERT_VEILEDER))
                .withRequestBody(containing(FNR))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withBody(denyResponse)
                )
        );
    }

    private void gittPermitResponseEnhet() {
        givenThat(post(urlEqualTo("/"))
                .withRequestBody(containing(PRIVELIGERT_VEILEDER))
                .withRequestBody(containing(RESOURCE_VEILARB_KONTOR_LAAS))
                .withRequestBody(containing(ENHET))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withBody(permitResponse)
                )
        );
    }

    private void gittDenyResponseEnhet() {
        givenThat(post(urlEqualTo("/"))
                .withRequestBody(containing(LITE_PRIVELIGERT_VEILEDER))
                .withRequestBody(containing(RESOURCE_VEILARB_KONTOR_LAAS))
                .withRequestBody(containing(ENHET))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withBody(denyResponse)
                )
        );
    }

    String permitResponse = "{\n" +
            "  \"Response\": {\n" +
            "    \"Decision\": \"Permit\",\n" +
            "    \"Status\": {\n" +
            "      \"StatusCode\": {\n" +
            "        \"Value\": \"Value\",\n" +
            "        \"StatusCode\": {\n" +
            "          \"Value\": \"Value\"\n" +
            "        }\n" +
            "      }\n" +
            "    }\n" +
            "  }\n" +
            "}";

    String denyResponse = "{\n" +
            "  \"Response\": {\n" +
            "    \"Decision\": \"Deny\",\n" +
            "    \"Status\": {\n" +
            "      \"StatusCode\": {\n" +
            "        \"Value\": \"Value\",\n" +
            "        \"StatusCode\": {\n" +
            "          \"Value\": \"Value\"\n" +
            "        }\n" +
            "      }\n" +
            "    },\n" +
            "    \"AssociatedAdvice\": {\n" +
            "      \"Id\": \"Id\",\n" +
            "      \"AttributeAssignment\": [\n" +
            "        {\n" +
            "          \"AttributeId\": \"AttributeId\",\n" +
            "          \"Value\": \"Value\",\n" +
            "          \"Category\": \"Category\",\n" +
            "          \"DataType\": \"DataType\"\n" +
            "        }\n" +
            "      ]\n" +
            "    }\n" +
            "  }\n" +
            "}";

}
