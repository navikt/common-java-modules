package no.nav.common.client.utils.graphql;

public class GraphqlNoDataException extends RuntimeException {

    public GraphqlNoDataException() {
        super("Graphql response mangler data");
    }

    public GraphqlNoDataException(String message) {
        super(message);
    }

}
