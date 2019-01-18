package no.nav.common.auth.openam.sbs;

import lombok.Builder;
import lombok.Value;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public interface OpenAMEventListener {

    Logger LOG = LoggerFactory.getLogger(OpenAMEventListener.class);
    String OPENAM_GENERAL_ERROR = "Could not get user attributes from OpenAM. ";

    default void fetchingUserAttributesFailed(OpenAmResponse openAmResponse) {
        LOG.error("{}: {}", OPENAM_GENERAL_ERROR, openAmResponse);
    }

    default void missingUserAttribute(String attribute) {
        LOG.error("{}: Response did not contain attribute: {}", OPENAM_GENERAL_ERROR, attribute);
    }

    @Value
    @Builder
    class OpenAmResponse {
        public final int status;
        public final String phrase;
        public final String content;
    }

    class DefaultOpenAMEventListener implements OpenAMEventListener {
    }
}
