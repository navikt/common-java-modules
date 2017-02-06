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
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
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
        when(pdpService.askForPermission(any(XacmlRequest.class))).thenReturn(new XacmlResponse().withDecision(Decision.Permit));

        final BiasedDecisionResponse biasedDecisionResponse = pep.evaluateWithBias("eyJpc3MiOiJuYXYubm8iLCJleHAiOjE0ODQ2NTI2NzIsImp0aSI6IkZHdXJVYWdleFRwTUVZTjdMRHlsQ1EiLCJpYXQiOjE0ODQ2NTIwNzIsIm5iZiI6MTQ4NDY1MTk1Miwic3ViIjoiYTExMTExMSJ9",
                null, "Foreldrepenger", "01010122222", "srvEksempelPep");

        assertThat(biasedDecisionResponse.getBiasedDecision(), is(Decision.Permit));
    }

    @Test
    public void returnsDenyForNotApplicable() {
        when(pdpService.askForPermission(any(XacmlRequest.class))).thenReturn(new XacmlResponse().withDecision(Decision.NotApplicable));

        final BiasedDecisionResponse biasedDecisionResponse = pep.evaluateWithBias("eyJpc3MiOiJuYXYubm8iLCJleHAiOjE0ODQ2NTI2NzIsImp0aSI6IkZHdXJVYWdleFRwTUVZTjdMRHlsQ1EiLCJpYXQiOjE0ODQ2NTIwNzIsIm5iZiI6MTQ4NDY1MTk1Miwic3ViIjoiYTExMTExMSJ9",
                null, "Foreldrepenger", "01010122222", "srvEksempelPep");

        assertThat(biasedDecisionResponse.getBiasedDecision(), is(Decision.Deny));
    }

    @Test(expected = PepException.class)
    public void decisionIndetminateThrowsException() {
        when(pdpService.askForPermission(any(XacmlRequest.class))).thenReturn(new XacmlResponse().withDecision(Decision.Indeterminate));

        final BiasedDecisionResponse biasedDecisionResponse = pep.evaluateWithBias("eyJpc3MiOiJuYXYubm8iLCJleHAiOjE0ODQ2NTI2NzIsImp0aSI6IkZHdXJVYWdleFRwTUVZTjdMRHlsQ1EiLCJpYXQiOjE0ODQ2NTIwNzIsIm5iZiI6MTQ4NDY1MTk1Miwic3ViIjoiYTExMTExMSJ9",
                null, "Foreldrepenger", "01010122222", "srvEksempelPep");

        assertThat(biasedDecisionResponse.getBiasedDecision(), is(Decision.Deny));
    }

    @Test
    public void buildsCorrectRequest() {
        pep.setClientValues("eyJpc3MiOiJuYXYubm8iLCJleHAiOjE0ODQ2NTI2NzIsImp0aSI6IkZHdXJVYWdleFRwTUVZTjdMRHlsQ1EiLCJpYXQiOjE0ODQ2NTIwNzIsIm5iZiI6MTQ4NDY1MTk1Miwic3ViIjoiYTExMTExMSJ9",
                null, "Foreldrepenger", "01010122222", "srvEksempelPep");
        XacmlRequest xacmlRequest = pep.createXacmlRequest();
        assertNotNull(xacmlRequest);
    }
}