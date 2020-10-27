package no.nav.common.client.utils.graphql;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class GraphqlRequest<V> {
    String query;
    V variables;
}
