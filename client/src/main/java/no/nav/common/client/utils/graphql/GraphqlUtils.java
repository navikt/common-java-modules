package no.nav.common.client.utils.graphql;

import lombok.extern.slf4j.Slf4j;
import no.nav.common.json.JsonUtils;

@Slf4j
public class GraphqlUtils {

    public static void throwIfErrorOrMissingData(GraphqlResponse<?> response) {
        throwIfError(response);
        throwIfMissingData(response);
    }

    public static void logWarningIfError(GraphqlResponse<?> response) {
        if (response.getErrors() != null) {
            log.warn("Graphql response feilet med feilmelding: " + JsonUtils.toJson(response.getErrors()));
        }
    }

    public static void throwIfError(GraphqlResponse<?> response) {
        if (response.getErrors() != null) {
            log.error("Graphql response feilet med feilmelding: " + JsonUtils.toJson(response.getErrors()));
            throw new GraphqlErrorException(response.errors);
        }
    }

    public static void throwIfMissingData(GraphqlResponse<?> response) {
        if (response.getData() == null) {
            log.error("Graphql response mangler data");
            throw new GraphqlNoDataException();
        }
    }
}
