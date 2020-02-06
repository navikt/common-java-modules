package no.nav.fasit;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class AzureOidcConfig {
    AzureOidcConfigProperties properties;
}
