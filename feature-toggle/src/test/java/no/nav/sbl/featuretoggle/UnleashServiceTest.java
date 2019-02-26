package no.nav.sbl.featuretoggle;

import com.github.tomakehurst.wiremock.junit.WireMockRule;
import no.finn.unleash.util.UnleashConfig;
import no.finn.unleash.util.UnleashScheduledExecutor;
import no.nav.sbl.featuretoggle.unleash.UnleashService;
import no.nav.sbl.featuretoggle.unleash.UnleashServiceConfig;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ScheduledFuture;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static java.util.Arrays.asList;
import static javax.ws.rs.core.HttpHeaders.CONTENT_TYPE;
import static javax.ws.rs.core.MediaType.APPLICATION_JSON;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

public class UnleashServiceTest {

    @Rule
    public WireMockRule wireMockRule = new WireMockRule(wireMockConfig().dynamicPort(),false);


    @Before
    public void setup() {
        givenThat(post(urlEqualTo("/client/register"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader(CONTENT_TYPE, APPLICATION_JSON)
                        .withBody("{}"))
        );

        createService();
    }

    private UnleashService createService() {
        return new UnleashService(UnleashServiceConfig.builder()
                .applicationName("test")
                .unleashApiUrl("http://localhost:" + wireMockRule.port())
                .unleashBuilderFactory(() -> UnleashConfig.builder().scheduledExecutor(new TestExecutor()))
                .build()
        );
    }

    @Test
    public void isEnabled() {
        assertThat(createService().isEnabled("test-toggle")).isFalse();
    }

    @Test
    public void ping_ok() {
        givenThat(get(urlEqualTo("/client/features"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader(CONTENT_TYPE, APPLICATION_JSON)
                        .withBody("{\"features\":[]}"))
        );
        UnleashService unleashService = createService();
        assertThat(unleashService.ping().erVellykket()).isTrue();
    }

    @Test
    public void ping_error() {
        UnleashService unleashService = createService();

        asList(404, 500).forEach(errorStatus -> {
            givenThat(get(urlEqualTo("/client/features"))
                    .willReturn(aResponse().withStatus(errorStatus))
            );
            assertThat(unleashService.ping().erVellykket()).isFalse();
        });

        wireMockRule.stop();
        assertThat(unleashService.ping().erVellykket()).isFalse();
    }

    private static class TestExecutor implements UnleashScheduledExecutor {
        @Override
        public ScheduledFuture setInterval(Runnable runnable, long l, long l1) throws RejectedExecutionException {
            return scheduleOnce(runnable);
        }

        @Override
        public ScheduledFuture scheduleOnce(Runnable runnable) {
            runnable.run();
            return mock(ScheduledFuture.class);
        }
    }

}