package no.nav.apiapp.config;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class StsConfig {

    public String url;
    public String username;
    public String password;

}
