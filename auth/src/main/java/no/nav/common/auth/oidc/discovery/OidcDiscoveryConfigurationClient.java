package no.nav.common.auth.oidc.discovery;

import no.nav.common.rest.RestUtils;

import javax.ws.rs.client.Client;
import javax.ws.rs.core.Response;

public class OidcDiscoveryConfigurationClient {

	private final Client client;

	public OidcDiscoveryConfigurationClient() {
		this(RestUtils.createClient());
	}

	public OidcDiscoveryConfigurationClient(Client client) {
		this.client = client;
	}

	public OidcDiscoveryConfiguration fetchDiscoveryConfiguration(String discoveryUrl) {
		Response response = client
				.target(discoveryUrl)
				.request()
				.get();

		return response.readEntity(OidcDiscoveryConfiguration.class);
	}

}
