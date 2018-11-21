package no.nav.dialogarena.config.fasit.dto;

import lombok.Data;
import lombok.experimental.Accessors;
import no.nav.dialogarena.config.fasit.client.LoadBalancerConfigDTO;

import javax.ws.rs.core.GenericType;
import java.util.List;

@Data
@Accessors(chain = true)
public class QueueManagerDTO {
    public static final GenericType<List<QueueManagerDTO>> LIST_TYPE = new GenericType<List<QueueManagerDTO>>() {};

    public Properties properties;
    public Scope scope;

    @Data
    @Accessors(chain = true)
    public static class Properties {
        public String hostname;
        public int port;
        public String name;
    }

    @Data
    @Accessors(chain = true)
    public static class Scope {
        public String environment;
    }

}
