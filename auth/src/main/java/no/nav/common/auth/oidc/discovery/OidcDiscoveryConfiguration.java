package no.nav.common.auth.oidc.discovery;

import com.google.gson.annotations.SerializedName;

public class OidcDiscoveryConfiguration {

	@SerializedName("jwks_uri")
	public String jwksUri;

	@SerializedName("token_endpoint")
	public String tokenEndpoint;

	@SerializedName("authorization_endpoint")
	public String authorizationEndpoint;

	public String issuer;

}

