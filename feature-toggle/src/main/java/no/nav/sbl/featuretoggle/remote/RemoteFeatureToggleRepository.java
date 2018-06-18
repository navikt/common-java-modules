package no.nav.sbl.featuretoggle.remote;

import com.fasterxml.jackson.core.type.TypeReference;
import no.nav.json.JsonUtils;
import no.nav.sbl.rest.RestUtils;

import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.Response;
import java.util.Map;

/**
 * @deprecated current recommendation is to use Unleash for feature toggling, see UnleashService
 */
@Deprecated
public class RemoteFeatureToggleRepository {
    private static final TypeReference<Map<String, Map<String, Boolean>>> type = new TypeReference<Map<String, Map<String, Boolean>>>() {
    };

    private static final RestUtils.RestConfig defaultConfig = RestUtils.RestConfig.builder()
            .connectTimeout(500)
            .readTimeout(500)
            .build();

    private WebTarget client;
    private String etag;
    private Map<String, Map<String, Boolean>> cache;

    public RemoteFeatureToggleRepository(String remoteUrl) {
        this(defaultConfig, remoteUrl);
    }

    public RemoteFeatureToggleRepository(RestUtils.RestConfig config, String remoteUrl) {
        this.client = RestUtils.createClient(config).target(remoteUrl);
    }

    public Map<String, Map<String, Boolean>> get() {
        Response response = this.client.request()
                .header("If-None-Match", etag)
                .get();

        if (response.getStatus() == 200) {
            this.etag = response.getHeaderString("etag");
            this.cache = JsonUtils.fromJson(response.readEntity(String.class), type);
        } else if (response.getStatus() != Response.Status.NOT_MODIFIED.getStatusCode()) {
            this.etag = null;
            this.cache = null;
        }

        return this.cache;
    }
}
