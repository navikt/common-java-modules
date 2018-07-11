package no.nav.dialogarena.config.fasit.client;

import lombok.Data;
import lombok.experimental.Accessors;

import javax.ws.rs.core.GenericType;
import java.util.List;

@Data
@Accessors(chain = true)
public class WebServiceEndpointDTO {
    public static final GenericType<List<WebServiceEndpointDTO>> LIST_TYPE = new GenericType<List<WebServiceEndpointDTO>>() {};

    public Properties properties;

    @Data
    @Accessors(chain = true)
    public static class Properties {
        public String endpointUrl;
    }

}
