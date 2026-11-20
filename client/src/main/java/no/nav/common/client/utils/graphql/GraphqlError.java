package no.nav.common.client.utils.graphql;

import java.util.List;
import lombok.Data;

@Data
public class GraphqlError {
    String message;
    List<Location> locations;
    List<String> path;
    Extensions extensions;

    @Data
    public static class Location {
        int line;
        int column;
    }

    @Data
    public static class Extensions {
        String code;
        String classification;
    }
}
