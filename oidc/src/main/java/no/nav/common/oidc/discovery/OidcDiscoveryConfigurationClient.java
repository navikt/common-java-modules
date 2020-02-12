package no.nav.common.oidc.discovery;

import no.nav.sbl.rest.RestUtils;

import javax.ws.rs.core.Response;

public class OidcDiscoveryConfigurationClient {

	public OidcDiscoveryConfiguration fetchDiscoveryConfiguration(String discoveryUrl) {
		Response response = RestUtils.createClient()
				.target(discoveryUrl)
				.request()
				.get();

		return response.readEntity(OidcDiscoveryConfiguration.class);
	}

}
