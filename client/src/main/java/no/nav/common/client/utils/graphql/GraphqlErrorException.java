package no.nav.common.client.utils.graphql;

import java.util.List;

public class GraphqlErrorException extends RuntimeException {

    private final List<GraphqlError> errors;

    public GraphqlErrorException(String message, List<GraphqlError> errors) {
        super(message);
        this.errors = errors;
    }

    public GraphqlErrorException(List<GraphqlError> errors) {
        this("Graphql response contains errors", errors);
    }

    public List<GraphqlError> getErrors() {
        return errors;
    }

}
