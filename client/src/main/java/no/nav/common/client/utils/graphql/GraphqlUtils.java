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
            log.warn("Graphql request feilet med feilmelding: " + JsonUtils.toJson(response.getErrors()));
        }
    }

    public static void throwIfError(GraphqlResponse<?> response) {
        if (response.getErrors() != null) {
            log.error("Graphql request feilet med feilmelding: " + JsonUtils.toJson(response.getErrors()));
            throw new RuntimeException("Graphql request feilet");
        }
    }

    public static void throwIfMissingData(GraphqlResponse<?> response) {
        if (response.getData() == null) {
            log.error("Graphql request mangler data");
            throw new RuntimeException("Graphql request mangler data");
        }
    }
}
