package no.nav.sbl.dialogarena.common.abac.pep;


import no.nav.sbl.dialogarena.common.abac.PdpService;
import no.nav.sbl.dialogarena.common.abac.pep.xacml.BiasedDecisionResponse;
import no.nav.sbl.dialogarena.common.abac.pep.xacml.Decision;
import org.junit.Before;
import org.junit.Test;
import org.mockito.*;

import java.util.ArrayList;
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
        pep.setClientValues(OIDC_TOKEN, SUBJECT_ID, DOMAIN, FNR, CREDENTIAL_RESOURCE);
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
    public void buildsCorrectEnvironmentWithOidcToken() {
        Environment environment = pep.makeEnvironment();
        List<Attribute> testAttribute = new ArrayList<>();
        testAttribute.add(new Attribute(StaticRequestValues.OIDCTOKEN_ID, OIDC_TOKEN));
        testAttribute.add(new Attribute(StaticRequestValues.CREDENTIALRESOURCE_ID, CREDENTIAL_RESOURCE));
        assertThat(environment.getAttribute(), is(testAttribute));
    }

    @Test
    public void buildsCorrectEnvironmentWithoutOidcToken() {
        pep.setClientValues(null, SUBJECT_ID, DOMAIN, FNR, CREDENTIAL_RESOURCE);
        Environment environment = pep.makeEnvironment();
        List<Attribute> testAttribute = new ArrayList<>();
        testAttribute.add(new Attribute(StaticRequestValues.CREDENTIALRESOURCE_ID, CREDENTIAL_RESOURCE));
        assertThat(environment.getAttribute(), is(testAttribute));
    }

    @Test
    public void buildsCorrectResource() {
        Resource resource = pep.makeResource();
        List<Attribute> testAttributes = new ArrayList<>();
        testAttributes.add(new Attribute(StaticRequestValues.RESOURCETYPE_ID, StaticRequestValues.RESOURCETYPE_VALUE));
        testAttributes.add(new Attribute(StaticRequestValues.DOMAIN_ID, DOMAIN));
        testAttributes.add(new Attribute(StaticRequestValues.FNR_ID, FNR));
        assertThat(resource.getAttribute(), is(testAttributes));
    }

    @Test
    public void buildsCorrectAccessSubject() {
        AccessSubject accessSubject = pep.makeAccessSubject();
        List<Attribute> testAttributes = new ArrayList<>();
        testAttributes.add(new Attribute(StaticRequestValues.SUBJECTID_ID, SUBJECT_ID));
        testAttributes.add(new Attribute(StaticRequestValues.SUBJECTTYPE_ID, StaticRequestValues.SUBJECTTYPE_VALUE));
        assertThat(accessSubject.getAttribute(), is(testAttributes));
    }

    @Test
    public void buildsCorrectAction() {
        Action action = pep.makeAction();
        List<Attribute> testAttributes = new ArrayList<>();
        testAttributes.add(new Attribute(StaticRequestValues.ACTIONID_ID, StaticRequestValues.ACTIONID_VALUE));
        assertThat(action.getAttribute(), is(testAttributes));
    }

    @Test
    public void buildsCorrectRequest() {
        Request request = pep.makeRequest();
        assertNotNull(request);
    }

    @Test
    public void requestReturnsNullWithNonValidValues() {
        pep.setClientValues(OIDC_TOKEN, SUBJECT_ID, null, FNR, CREDENTIAL_RESOURCE);
        Request request = pep.makeRequest();
        assertNull(request);
    }

    @Test
    public void buildsCorrectXacmlRequest() {
        XacmlRequest xacmlRequest = pep.makeXacmlRequest();
        assertNotNull(xacmlRequest);
    }
}