package no.nav.common.cxf;

import lombok.Builder;
import lombok.Getter;

@Builder
@Getter
public class StsConfig {
    String url;
    String username;
    String password;
}
