package no.nav.dialogarena.config.fasit.dto;

import lombok.Data;
import lombok.experimental.Accessors;

import javax.ws.rs.core.GenericType;
import java.util.List;

@Data
@Accessors(chain = true)
public class QueueDTO {
    public static final GenericType<List<QueueDTO>> LIST_TYPE = new GenericType<List<QueueDTO>>() {};

    public Scope scope;
    public Properties properties;

    @Data
    @Accessors(chain = true)
    public static class Properties {
        public String queueName;
    }

    @Data
    @Accessors(chain = true)
    public static class Scope {
        public String environment;
        public String environmentclass;
    }

}
