package no.nav.common.oidc;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.SneakyThrows;
import org.apache.http.HttpEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.util.EntityUtils;

public class OidcDiscoveryConfigurationClient {

	private final CloseableHttpClient httpClient = HttpClientBuilder.create().useSystemProperties().build();

	private final ObjectMapper jsonMapper = new ObjectMapper();

	@SneakyThrows
	public OidcDiscoveryConfiguration fetchDiscoveryConfiguration(String discoveryUrl) {

		try (CloseableHttpResponse response = httpClient.execute(new HttpGet(discoveryUrl))) {
			HttpEntity entity = response.getEntity();

			if (entity != null) {
				String jsonData = EntityUtils.toString(entity);
				return jsonMapper.readValue(jsonData, OidcDiscoveryConfiguration.class);
			}

			throw new IllegalArgumentException("HttpResponse is missing entity");
		}

	}

}
