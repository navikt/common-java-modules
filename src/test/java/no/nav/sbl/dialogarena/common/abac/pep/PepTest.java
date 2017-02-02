package no.nav.sbl.dialogarena.common.abac.pep;


import no.nav.sbl.dialogarena.common.abac.PdpService;
import no.nav.sbl.dialogarena.common.abac.pep.xacml.BiasedDecisionResponse;
import no.nav.sbl.dialogarena.common.abac.pep.xacml.Decision;
import org.junit.Before;
import org.junit.Test;
import org.mockito.*;

import static no.nav.sbl.dialogarena.common.abac.pep.MockXacmlRequest.getXacmlRequest;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

public class PepTest {

    @InjectMocks
    Pep pep;

    @Mock
    PdpService pdpService;

    @Before
    public void setup() {
        MockitoAnnotations.initMocks(this);
    }

    @Test
    public void returnsDecision() {
        when(pdpService.askForPermission(any(XacmlRequest.class))).thenReturn(new XacmlResponse().withResponse(Decision.Permit));

        final XacmlRequest xacmlRequest = getXacmlRequest();

        final BiasedDecisionResponse biasedDecisionResponse = pep.evaluateWithBias(xacmlRequest);

        assertThat(biasedDecisionResponse.getBiasedDecision(), is(Decision.Permit));
    }

    @Test
    public void returnsDenyForNotApplicable() {
        when(pdpService.askForPermission(any(XacmlRequest.class))).thenReturn(new XacmlResponse().withResponse(Decision.NotApplicable));

        final XacmlRequest xacmlRequest = getXacmlRequest();

        final BiasedDecisionResponse biasedDecisionResponse = pep.evaluateWithBias(xacmlRequest);

        assertThat(biasedDecisionResponse.getBiasedDecision(), is(Decision.Deny));
    }

    @Test(expected = PepException.class)
    public void decisionIndetminateThrowsException() {
        when(pdpService.askForPermission(any(XacmlRequest.class))).thenReturn(new XacmlResponse().withResponse(Decision.Indeterminate));

        final XacmlRequest xacmlRequest = getXacmlRequest();

        final BiasedDecisionResponse biasedDecisionResponse = pep.evaluateWithBias(xacmlRequest);

        assertThat(biasedDecisionResponse.getBiasedDecision(), is(Decision.Deny));
    }


}