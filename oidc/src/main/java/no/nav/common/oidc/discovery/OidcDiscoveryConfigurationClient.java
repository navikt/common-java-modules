package no.nav.common.oidc.discovery;

import no.nav.sbl.rest.RestUtils;

import javax.ws.rs.core.Response;
import java.util.Optional;

public class OidcDiscoveryConfigurationClient {

	public Optional<OidcDiscoveryConfiguration> fetchDiscoveryConfiguration(String discoveryUrl) {
		try {
			Response response = RestUtils.createClient()
					.target(discoveryUrl)
					.request()
					.get();

			return Optional.of(response.readEntity(OidcDiscoveryConfiguration.class));
		} catch (Exception e) {
			return Optional.empty();
		}
	}

}
