package no.nav.dialogarena.config.fasit;

import lombok.Data;
import lombok.experimental.Accessors;

@Data
@Accessors(chain = true)
public class LoadBalancerConfig {
    public String contextRoots;
    public String url;
    public String environment;
}
