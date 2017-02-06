package no.nav.sbl.dialogarena.common.abac.pep;


import no.nav.sbl.dialogarena.common.abac.PdpService;
import no.nav.sbl.dialogarena.common.abac.pep.xacml.BiasedDecisionResponse;
import no.nav.sbl.dialogarena.common.abac.pep.xacml.Decision;
import org.junit.Before;
import org.junit.Test;
import org.mockito.*;

import java.util.List;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

public class PepTest {

    private static final String OIDC_TOKEN = "eyJpc3MiOiJuYXYubm8iLCJleHAiOjE0ODQ2NTI2NzIsImp0aSI6IkZHdXJVYWdleFRwTUVZTjdMRHlsQ1EiLCJpYXQiOjE0ODQ2NTIwNzIsIm5iZiI6MTQ4NDY1MTk1Miwic3ViIjoiYTExMTExMSJ9";
    private static final String SUBJECT_ID = "A111111";
    private static final String DOMAIN = "Foreldrepenger";
    private static final String FNR = "01010122222";
    private static final String CREDENTIAL_RESOURCE = "srvEksempelPep";

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

        final BiasedDecisionResponse biasedDecisionResponse = pep.evaluateWithBias(OIDC_TOKEN, SUBJECT_ID, DOMAIN, FNR, CREDENTIAL_RESOURCE);

        assertThat(biasedDecisionResponse.getBiasedDecision(), is(Decision.Permit));
    }

    @Test
    public void returnsDenyForNotApplicable() {
        when(pdpService.askForPermission(any(XacmlRequest.class))).thenReturn(new XacmlResponse().withDecision(Decision.NotApplicable));

        final BiasedDecisionResponse biasedDecisionResponse = pep.evaluateWithBias(OIDC_TOKEN, SUBJECT_ID, DOMAIN, FNR, CREDENTIAL_RESOURCE);

        assertThat(biasedDecisionResponse.getBiasedDecision(), is(Decision.Deny));
    }

    @Test(expected = PepException.class)
    public void decisionIndetminateThrowsException() {
        when(pdpService.askForPermission(any(XacmlRequest.class))).thenReturn(new XacmlResponse().withDecision(Decision.Indeterminate));

        final BiasedDecisionResponse biasedDecisionResponse = pep.evaluateWithBias(OIDC_TOKEN, SUBJECT_ID, DOMAIN, FNR, CREDENTIAL_RESOURCE);

        assertThat(biasedDecisionResponse.getBiasedDecision(), is(Decision.Deny));
    }

    @Test
    public void buildsCorrectEnvironment() {
        pep.setClientValues(OIDC_TOKEN, SUBJECT_ID, DOMAIN, FNR, CREDENTIAL_RESOURCE);
        Environment environment = pep.getEnvironment();
        List<Attribute> environmentAttributes = environment.getAttribute();
        assertThat(environmentAttributes.size(), is(2));
        assertThat(environmentAttributes.get(0).getValue(), is(OIDC_TOKEN));
        assertThat(environmentAttributes.get(1).getValue(), is(CREDENTIAL_RESOURCE));
    }

    @Test
    public void buildsCorrectResource() {
        pep.setClientValues(OIDC_TOKEN, SUBJECT_ID, DOMAIN, FNR, CREDENTIAL_RESOURCE);
        Resource resource = pep.getResource();
        List<Attribute> resourceAttributes = resource.getAttribute();
        assertThat(resourceAttributes.size(), is(3));
        assertThat(resourceAttributes.get(0).getAttributeId(), is( "no.nav.abac.attributter.resource.felles.resource_type"));
        assertThat(resourceAttributes.get(1).getValue(), is(DOMAIN));
        assertThat(resourceAttributes.get(2).getValue(), is(FNR));
    }

    @Test
    public void buildsCorrectAccessSubject() {
        pep.setClientValues(OIDC_TOKEN, SUBJECT_ID, DOMAIN, FNR, CREDENTIAL_RESOURCE);
        AccessSubject accessSubject = pep.getAccessSubject();
        assertThat(accessSubject.getAttribute().get(0).getValue(), is(SUBJECT_ID));
    }

    @Test
    public void buildsCorrectAction() {
        Action action = pep.getAction();
        assertThat(action.getAttribute().get(0).getValue(), is("read"));
    }

    @Test
    public void buildsCorrectRequest() {
        pep.setClientValues(OIDC_TOKEN, SUBJECT_ID, DOMAIN, FNR, CREDENTIAL_RESOURCE);
        Request request = pep.buildRequest();
        assertNotNull(request);
    }

    @Test
    public void requestReturnsNullWithNonValidValues() {
        Request request = pep.buildRequest();
        assertNull(request);
    }

    @Test
    public void buildsCorrectXacmlRequest() {
        pep.setClientValues(OIDC_TOKEN, SUBJECT_ID, DOMAIN, FNR, CREDENTIAL_RESOURCE);
        XacmlRequest xacmlRequest = pep.createXacmlRequest();
        assertNotNull(xacmlRequest);
    }
}