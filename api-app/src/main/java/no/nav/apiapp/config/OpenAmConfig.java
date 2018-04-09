package no.nav.apiapp.config;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class OpenAmConfig {
    public String restUrl;
    public String username;
    public String password;
}
