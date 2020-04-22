package no.nav.common.featuretoggle.remote;

import okhttp3.HttpUrl;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;
import org.junit.Ignore;
import org.junit.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

public class RemoteFeatureToggleRepositoryTest {

    public static final String PATH = "/fo-feature";

    @Test
    public void gjor_kall_hver_gang_uten_etags() throws Exception {
        MockWebServer server = MockServer.lagMockServer(0, MockServer.MOCK_RESPONSE);
        server.start();
        HttpUrl url = server.url(PATH);

        RemoteFeatureToggleRepository repo = new RemoteFeatureToggleRepository(url.toString());
        Map<String, Map<String, Boolean>> map1 = repo.get();
        Map<String, Map<String, Boolean>> map2 = repo.get();

        RecordedRequest request1 = server.takeRequest();
        RecordedRequest request2 = server.takeRequest();

        assertThat(request1.getPath()).isEqualTo(PATH);
        assertThat(request2.getPath()).isEqualTo(PATH);

        // json deserialiser siden det ikke er noen etags, derfor nytt objekt
        assertThat(map1 == map2).isFalse();
    }

    @Test
    public void cache_nar_etags_matcher() throws Exception {
        MockWebServer server = MockServer.lagMockServer(0, MockServer.MOCK_RESPONSE, "etagvalue");
        server.start();
        HttpUrl url = server.url(PATH);

        RemoteFeatureToggleRepository repo = new RemoteFeatureToggleRepository(url.toString());
        Map<String, Map<String, Boolean>> map1 = repo.get();
        Map<String, Map<String, Boolean>> map2 = repo.get();

        RecordedRequest request = server.takeRequest();
        RecordedRequest request2 = server.takeRequest();

        assertThat(request.getPath()).isEqualTo(PATH);
        assertThat(request2.getPath()).isEqualTo(PATH);

        // Etags matchet, så ingen ny deserialisering og derfor samme objekt
        assertThat(map1 == map2).isTrue();
    }

    @Test
    @Ignore("Skal fungere, men vil ikke ha direkte avhengighet ut i miljø")
    public void cache_fungerer_mot_nginx() {
        RemoteFeatureToggleRepository repo = new RemoteFeatureToggleRepository("https://app-t6.adeo.no/feature");
        Map<String, Map<String, Boolean>> map1 = repo.get();
        Map<String, Map<String, Boolean>> map2 = repo.get();

        // Etags matchet, så ingen ny deserialisering og derfor samme objekt
        assertThat(map1 == map2).isTrue();
    }
}