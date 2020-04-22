package no.nav.common.featuretoggle.remote;

import okhttp3.HttpUrl;
import okhttp3.mockwebserver.MockWebServer;
import org.junit.Test;

import java.io.IOException;

import static org.assertj.core.api.Assertions.assertThat;

public class RemoteFeatureToggleTest {

    @Test
    public void skal_returnere_default_ved_random_feil() {
        RemoteFeatureToggle trueFeature = lagToggle("", "", true);
        RemoteFeatureToggle falseFeature = lagToggle("", "", false);

        assertThat(trueFeature.erAktiv()).isTrue();
        assertThat(falseFeature.erAktiv()).isFalse();
    }

    @Test
    public void skal_returnere_default_ved_timeout() throws IOException {
        MockWebServer server = MockServer.lagMockServer(1500, MockServer.MOCK_RESPONSE);
        server.start();
        HttpUrl url = server.url("/fo-feature");

        RemoteFeatureToggle featureToggle = lagToggle(url.toString(), "aktivitetsplan.kvp", true);
        assertThat(featureToggle.erAktiv()).isTrue();

        server.shutdown();
    }

    @Test
    public void skal_returnere_resultatet_fra_parser() throws IOException {
        MockWebServer server = MockServer.lagMockServer(0, "{\"aktivitetsplan\": {\"kvp\": false}}");
        server.start();
        HttpUrl url = server.url("/fo-feature");
        System.out.println(url);

        RemoteFeatureToggle featureToggle = lagToggle(url.toString(), "aktivitetsplan.kvp", true);
        assertThat(featureToggle.erAktiv()).isFalse();

        server.shutdown();
    }

    @Test
    public void skal_returnere_default_ved_ssl_feil() {
        RemoteFeatureToggleRepository repo = new RemoteFeatureToggleRepository("https://feature-t6.nais.preprod.local/fo-feature");
        RemoteFeatureToggle toggle = lagToggle(repo, "aktivitetsplan.kvp", true);

        assertThat(toggle.erAktiv()).isTrue();
    }

    static RemoteFeatureToggle lagToggle(RemoteFeatureToggleRepository repo, String toggleKey, boolean defaultAktiv) {
        return new RemoteFeatureToggle(repo, toggleKey, defaultAktiv);
    }

    static RemoteFeatureToggle lagToggle(String remoteUrl, String toggleKey, boolean defaultAktiv) {
        RemoteFeatureToggleRepository repo = new RemoteFeatureToggleRepository(remoteUrl);
        return new RemoteFeatureToggle(repo, toggleKey, defaultAktiv);
    }
}