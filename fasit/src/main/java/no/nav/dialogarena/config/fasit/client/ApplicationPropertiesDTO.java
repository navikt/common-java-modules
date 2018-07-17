package no.nav.dialogarena.config.fasit.client;

import javax.ws.rs.core.GenericType;
import java.util.List;

public class ApplicationPropertiesDTO {
    public static final GenericType<List<ApplicationPropertiesDTO>> LIST_TYPE = new GenericType<List<ApplicationPropertiesDTO>>() {};

    public Properties properties;

    public static class Properties {
        public String applicationProperties;
    }

}
