package no.nav.common.client.utils.graphql;

import lombok.Data;

import java.util.List;

@Data
public class GraphqlResponse<D> {
    List<GraphqlError> errors;
    D data;
}
