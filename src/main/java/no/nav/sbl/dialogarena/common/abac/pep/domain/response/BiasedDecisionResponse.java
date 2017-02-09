package no.nav.sbl.dialogarena.common.abac.pep.domain.response;

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


}
