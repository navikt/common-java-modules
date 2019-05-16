package no.nav.fasit;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class AzureOidcConfigProperties {
    String clientId;
    String callbackUri;
    String discoveryUri;
}
