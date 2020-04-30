package no.nav.common.auth.oidc.discovery;

import no.nav.common.rest.RestUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.client.Client;
import javax.ws.rs.core.Response;

public class OidcDiscoveryConfigurationClient {

	private static final Logger logger = LoggerFactory.getLogger(OidcDiscoveryConfigurationClient.class);

	private final Client client;

	public OidcDiscoveryConfigurationClient() {
		this(RestUtils.createClient());
	}

	public OidcDiscoveryConfigurationClient(Client client) {
		this.client = client;
	}

	public OidcDiscoveryConfiguration fetchDiscoveryConfiguration(String discoveryUrl) {
		try {
			Response response = client
					.target(discoveryUrl)
					.request()
					.get();

			return response.readEntity(OidcDiscoveryConfiguration.class);
		} catch (Exception e) {
			logger.error("Failed to retrieve discovery configuration from " + discoveryUrl, e);
			throw e;
		}
	}

}
