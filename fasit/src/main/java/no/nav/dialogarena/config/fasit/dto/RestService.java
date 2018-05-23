package no.nav.dialogarena.config.fasit.dto;

import javax.ws.rs.core.GenericType;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class RestService {

    public static final GenericType<List<RestService>> LIST_TYPE = new GenericType<List<RestService>>() {};

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
