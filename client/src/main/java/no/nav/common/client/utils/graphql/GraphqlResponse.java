package no.nav.common.client.utils.graphql;

import java.util.List;
import lombok.Data;

@Data
public class GraphqlResponse<D> {
    List<GraphqlError> errors;
    D data;
}
