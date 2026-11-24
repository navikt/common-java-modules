package no.nav.common.featuretoggle;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static jakarta.ws.rs.core.HttpHeaders.CONTENT_TYPE;
import static jakarta.ws.rs.core.MediaType.APPLICATION_JSON;
import static java.util.Arrays.asList;
import static org.assertj.core.api.Assertions.assertThat;

import com.github.tomakehurst.wiremock.WireMockServer;
import io.getunleash.util.UnleashConfig;
import io.getunleash.util.UnleashScheduledExecutor;
import java.util.Collections;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Future;
import java.util.concurrent.RejectedExecutionException;
import org.jspecify.annotations.NonNull;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class UnleashClientImplTest {

    public WireMockServer wireMockRule = new WireMockServer(wireMockConfig().dynamicPort());

    @BeforeEach
    public void setup() {
        wireMockRule.start();
        wireMockRule.stubFor(post(urlEqualTo("/client/register"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader(CONTENT_TYPE, APPLICATION_JSON)
                        .withBody("{}")));

        createService();
    }

    @AfterEach
    public void stopp_server() {
        wireMockRule.stop();
    }

    private UnleashClientImpl createService() {
        UnleashConfig.Builder configBuilder = UnleashConfig.builder()
                .appName("test")
                .unleashAPI("http://localhost:" + wireMockRule.port())
                .synchronousFetchOnInitialisation(false)
                .scheduledExecutor(new TestExecutor());

        return new UnleashClientImpl(configBuilder, Collections.emptyList());
    }

    @Test
    public void isEnabled() {
        assertThat(createService().isEnabled("test-toggle")).isFalse();
    }

    @Test
    public void ping_ok() {
        wireMockRule.stubFor(get(urlEqualTo("/client/features"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader(CONTENT_TYPE, APPLICATION_JSON)
                        .withBody("{\"features\":[]}")));
        UnleashClientImpl unleashClientImpl = createService();
        assertThat(unleashClientImpl.checkHealth().isHealthy()).isTrue();
    }

    @Test
    public void ping_error() {
        UnleashClientImpl unleashClientImpl = createService();

        asList(404, 500).forEach(errorStatus -> {
            wireMockRule.stubFor(
                    get(urlEqualTo("/client/features")).willReturn(aResponse().withStatus(errorStatus)));
            assertThat(unleashClientImpl.checkHealth().isHealthy()).isFalse();
        });

        wireMockRule.stop();
        assertThat(unleashClientImpl.checkHealth().isHealthy()).isFalse();
    }

    private static class TestExecutor implements UnleashScheduledExecutor {
        @Override
        public void setInterval(@NonNull Runnable runnable, long l, long l1) throws RejectedExecutionException {
            scheduleOnce(runnable);
        }

        @Override
        public @NonNull Future<Void> scheduleOnce(@NonNull Runnable runnable) {
            runnable.run();
            return CompletableFuture.completedFuture(null);
        }
    }
}
