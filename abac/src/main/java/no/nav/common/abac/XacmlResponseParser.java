package no.nav.common.abac;

import no.nav.common.abac.domain.response.Decision;
import no.nav.common.abac.domain.response.XacmlResponse;
import no.nav.common.abac.exception.PepException;

public class XacmlResponseParser {

    private static final int NUMBER_OF_RESPONSES_ALLOWED = 1;

    public static boolean harTilgang(XacmlResponse response) {
        return getSingleDecision(response) == Decision.Permit;
    }

    public static Decision getSingleDecision(XacmlResponse response) {
        if (response.getResponse().size() > NUMBER_OF_RESPONSES_ALLOWED) {
            throw new PepException("Pep is giving " + response.getResponse().size() + " responses. Only "
                    + NUMBER_OF_RESPONSES_ALLOWED + " is supported.");
        }

        return response.getResponse().get(0).getDecision();
    }

}
