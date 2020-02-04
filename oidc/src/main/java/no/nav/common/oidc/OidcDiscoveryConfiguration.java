package no.nav.common.oidc;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public class OidcDiscoveryConfiguration {

	@JsonProperty("jwks_uri")
	public String jwksUri;

	@JsonProperty("token_endpoint")
	public String tokenEndpoint;

	@JsonProperty("authorization_endpoint")
	public String authorizationEndpoint;

	public String issuer;

}

