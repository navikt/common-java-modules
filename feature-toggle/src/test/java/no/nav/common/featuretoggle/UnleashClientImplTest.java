package no.nav.common.featuretoggle;

import com.github.tomakehurst.wiremock.junit.WireMockRule;
import no.finn.unleash.util.UnleashConfig;
import no.finn.unleash.util.UnleashScheduledExecutor;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import java.util.Collections;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ScheduledFuture;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static java.util.Arrays.asList;
import static javax.ws.rs.core.HttpHeaders.CONTENT_TYPE;
import static javax.ws.rs.core.MediaType.APPLICATION_JSON;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

public class UnleashClientImplTest {

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

    private UnleashClientImpl createService() {
        UnleashConfig.Builder configBuilder = UnleashConfig.builder()
                .appName("test")
                .unleashAPI("http://localhost:" + wireMockRule.port())
                .synchronousFetchOnInitialisation(true)
                .scheduledExecutor(new TestExecutor());

        return new UnleashClientImpl(configBuilder, Collections.emptyList());
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
        UnleashClientImpl unleashClientImpl = createService();
        assertThat(unleashClientImpl.checkHealth().isHealthy()).isTrue();
    }

    @Test
    public void ping_error() {
        UnleashClientImpl unleashClientImpl = createService();

        asList(404, 500).forEach(errorStatus -> {
            givenThat(get(urlEqualTo("/client/features"))
                    .willReturn(aResponse().withStatus(errorStatus))
            );
            assertThat(unleashClientImpl.checkHealth().isHealthy()).isFalse();
        });

        wireMockRule.stop();
        assertThat(unleashClientImpl.checkHealth().isHealthy()).isFalse();
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