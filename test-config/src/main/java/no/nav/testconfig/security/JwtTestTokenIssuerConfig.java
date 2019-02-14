package no.nav.testconfig.security;

import lombok.Builder;
import lombok.Value;
import lombok.experimental.Accessors;

@Builder
@Value
@Accessors(fluent = true)
public class JwtTestTokenIssuerConfig {
    public final String id;
    public final String issuer;
    public final String audience;
    public final String algorithm;
}
