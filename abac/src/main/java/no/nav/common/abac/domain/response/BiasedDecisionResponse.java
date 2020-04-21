package no.nav.common.abac.domain.response;

public class BiasedDecisionResponse {

    private final Decision biasedDecision;
    private final XacmlResponse xacmlResponse;

    public BiasedDecisionResponse(Decision biasedDecision, XacmlResponse xacmlResponse) {
        this.biasedDecision = biasedDecision;
        this.xacmlResponse = xacmlResponse;
    }

    public Decision getBiasedDecision() {
        return biasedDecision;
    }

    public XacmlResponse getXacmlResponse() {
        return xacmlResponse;
    }
}
