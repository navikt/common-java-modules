package no.nav.dialogarena.config.fasit.client;

import javax.ws.rs.core.GenericType;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class RestServiceDTO {

    public static final GenericType<List<RestServiceDTO>> LIST_TYPE = new GenericType<List<RestServiceDTO>>() {};

    private String alias;
    private Map<String, String> scope = new HashMap<>();
    private Map<String, String> properties = new HashMap<>();
    private Map<String, String> exposedby = new HashMap<>();

    public String getUrl() {
        return properties.get("url");
    }

    public String getAlias() {
        return alias;
    }

    public String getEnvironment() {
        return scope.get("environment");
    }

    public String getEnvironmentClass() {
        return scope.get("environmentclass");
    }

    public String getApplication() {
        return exposedby.get("application");
    }

}
