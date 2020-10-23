package no.nav.common.client.utils.graphql;

import no.nav.common.utils.FileUtils;

public class GraphqlRequestBuilder<V> {

    private final String graphqlRequestStr;

    public GraphqlRequestBuilder(String graphqlResourceFilePath) {
        this.graphqlRequestStr = FileUtils.getResourceFileAsString(graphqlResourceFilePath);
    }

    public GraphqlRequest<V> buildRequest(V variables) {
        return new GraphqlRequest<>(graphqlRequestStr, variables);
    }

}
