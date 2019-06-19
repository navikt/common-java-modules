package no.nav.fasit.client;

import lombok.Data;
import lombok.experimental.Accessors;

import javax.ws.rs.core.GenericType;
import java.util.List;

@Data
@Accessors(chain = true)
public class LoadBalancerConfigDTO {
    public static final GenericType<List<LoadBalancerConfigDTO>> LIST_TYPE = new GenericType<List<LoadBalancerConfigDTO>>() {};

    public Scope scope;
    public Properties properties;
    public String alias;

    @Data
    @Accessors(chain = true)
    public static class Properties {
        public String contextRoots;
        public String url;
    }

    @Data
    @Accessors(chain = true)
    public static class Scope {
        public String environment;
    }

}
