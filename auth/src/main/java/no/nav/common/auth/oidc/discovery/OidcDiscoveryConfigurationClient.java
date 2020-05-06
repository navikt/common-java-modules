package no.nav.common.auth.oidc.discovery;

import com.google.gson.Gson;
import lombok.SneakyThrows;
import no.nav.common.rest.client.RestClient;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static no.nav.common.rest.client.RestUtils.parseJsonResponseBodyOrThrow;

public class OidcDiscoveryConfigurationClient {

	private static final Logger log = LoggerFactory.getLogger(OidcDiscoveryConfigurationClient.class);

	private static final Gson gson = new Gson();

	private final OkHttpClient client;

	public OidcDiscoveryConfigurationClient() {
		this(RestClient.baseClient());
	}

	public OidcDiscoveryConfigurationClient(OkHttpClient client) {
		this.client = client;
	}

	@SneakyThrows
	public OidcDiscoveryConfiguration fetchDiscoveryConfiguration(String discoveryUrl) {
		Request request = new Request.Builder()
				.url(discoveryUrl)
				.get()
				.build();

		try (Response response = client.newCall(request).execute()) {
			return parseJsonResponseBodyOrThrow(response.body(), OidcDiscoveryConfiguration.class);
		} catch (Exception e) {
			log.error("Failed to retrieve discovery configuration from " + discoveryUrl, e);
			throw e;
		}
	}

}
