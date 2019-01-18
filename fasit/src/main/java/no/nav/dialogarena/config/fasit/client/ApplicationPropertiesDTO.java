package no.nav.dialogarena.config.fasit.client;

import lombok.Data;
import lombok.experimental.Accessors;

import javax.ws.rs.core.GenericType;
import java.util.List;

public class ApplicationPropertiesDTO {
    public static final GenericType<List<ApplicationPropertiesDTO>> LIST_TYPE = new GenericType<List<ApplicationPropertiesDTO>>() {};

    public Scope scope;
    public Properties properties;

    public static class Properties {
        public String applicationProperties;
    }

    @Data
    @Accessors(chain = true)
    public static class Scope {
        public String environment;
        public String environmentclass;
    }


}
