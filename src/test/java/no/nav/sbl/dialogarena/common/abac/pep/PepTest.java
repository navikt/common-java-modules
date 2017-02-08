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
        pep.withClientValues(MockXacmlRequest.OIDC_TOKEN, MockXacmlRequest.SUBJECT_ID, MockXacmlRequest.DOMAIN, MockXacmlRequest.FNR, MockXacmlRequest.CREDENTIAL_RESOURCE);
    }

    @Test
    public void returnsDecision() {
        when(pdpService.askForPermission(any(XacmlRequest.class))).thenReturn(new XacmlResponse().withDecision(Decision.Permit));

        final BiasedDecisionResponse biasedDecisionResponse = pep.evaluateWithBias(MockXacmlRequest.OIDC_TOKEN,
                MockXacmlRequest.SUBJECT_ID, MockXacmlRequest.DOMAIN, MockXacmlRequest.FNR, MockXacmlRequest.CREDENTIAL_RESOURCE);

        assertThat(biasedDecisionResponse.getBiasedDecision(), is(Decision.Permit));
    }

    @Test
    public void returnsDenyForNotApplicable() {
        when(pdpService.askForPermission(any(XacmlRequest.class))).thenReturn(new XacmlResponse().withDecision(Decision.NotApplicable));

        final BiasedDecisionResponse biasedDecisionResponse = pep.evaluateWithBias(MockXacmlRequest.OIDC_TOKEN, MockXacmlRequest.SUBJECT_ID,
                MockXacmlRequest.DOMAIN, MockXacmlRequest.FNR, MockXacmlRequest.CREDENTIAL_RESOURCE);

        assertThat(biasedDecisionResponse.getBiasedDecision(), is(Decision.Deny));
    }

    @Test(expected = PepException.class)
    public void decisionIndeterminateThrowsException() {
        when(pdpService.askForPermission(any(XacmlRequest.class))).thenReturn(new XacmlResponse().withDecision(Decision.Indeterminate));

        final BiasedDecisionResponse biasedDecisionResponse = pep.evaluateWithBias(MockXacmlRequest.OIDC_TOKEN, MockXacmlRequest.SUBJECT_ID,
                MockXacmlRequest.DOMAIN, MockXacmlRequest.FNR, MockXacmlRequest.CREDENTIAL_RESOURCE);

        assertThat(biasedDecisionResponse.getBiasedDecision(), is(Decision.Deny));
    }

    @Test
    public void buildsCorrectEnvironmentWithOidcToken() {
        Environment environment = pep.makeEnvironment();
        List<Attribute> expectedAttributes = new ArrayList<>();
        expectedAttributes.add(new Attribute(StaticRequestValues.OIDCTOKEN_ID, MockXacmlRequest.OIDC_TOKEN));
        expectedAttributes.add(new Attribute(StaticRequestValues.CREDENTIALRESOURCE_ID, MockXacmlRequest.CREDENTIAL_RESOURCE));

        assertThat(environment.getAttribute(), is(expectedAttributes));
    }

    @Test
    public void buildsCorrectEnvironmentWithoutOidcToken() {
        pep.withClientValues(null, MockXacmlRequest.SUBJECT_ID, MockXacmlRequest.DOMAIN, MockXacmlRequest.FNR, MockXacmlRequest.CREDENTIAL_RESOURCE);
        Environment environment = pep.makeEnvironment();
        List<Attribute> expectedAttributes = new ArrayList<>();
        expectedAttributes.add(new Attribute(StaticRequestValues.CREDENTIALRESOURCE_ID, MockXacmlRequest.CREDENTIAL_RESOURCE));

        assertThat(environment.getAttribute(), is(expectedAttributes));
    }

    @Test
    public void buildsCorrectResource() {
        Resource resource = pep.makeResource();
        List<Attribute> expectedAttributes = new ArrayList<>();
        expectedAttributes.add(new Attribute(StaticRequestValues.RESOURCETYPE_ID, StaticRequestValues.RESOURCETYPE_VALUE));
        expectedAttributes.add(new Attribute(StaticRequestValues.DOMAIN_ID, MockXacmlRequest.DOMAIN));
        expectedAttributes.add(new Attribute(StaticRequestValues.FNR_ID, MockXacmlRequest.FNR));

        assertThat(resource.getAttribute(), is(expectedAttributes));
    }

    @Test
    public void buildsCorrectAccessSubject() {
        AccessSubject accessSubject = pep.makeAccessSubject();
        List<Attribute> expectedAttributes = new ArrayList<>();
        expectedAttributes.add(new Attribute(StaticRequestValues.SUBJECTID_ID, MockXacmlRequest.SUBJECT_ID));
        expectedAttributes.add(new Attribute(StaticRequestValues.SUBJECTTYPE_ID, StaticRequestValues.SUBJECTTYPE_VALUE));

        assertThat(accessSubject.getAttribute(), is(expectedAttributes));
    }

    @Test
    public void buildsCorrectAction() {
        Action action = pep.makeAction();
        List<Attribute> expectedAttributes = new ArrayList<>();
        expectedAttributes.add(new Attribute(StaticRequestValues.ACTIONID_ID, StaticRequestValues.ACTIONID_VALUE));

        assertThat(action.getAttribute(), is(expectedAttributes));
    }

    @Test
    public void buildsCorrectRequestWithOidcToken() {
        pep.withClientValues(MockXacmlRequest.OIDC_TOKEN, null, MockXacmlRequest.DOMAIN, MockXacmlRequest.FNR, MockXacmlRequest.CREDENTIAL_RESOURCE);
        Request request = pep.makeRequest();
        Request expectedRequest = MockXacmlRequest.getRequest();

        assertThat(request, is(expectedRequest));
    }

    @Test
    public void buildsCorrectRequestWithSubjectId() {
        pep.withClientValues(null, MockXacmlRequest.SUBJECT_ID, MockXacmlRequest.DOMAIN, MockXacmlRequest.FNR, MockXacmlRequest.CREDENTIAL_RESOURCE);
        Request request = pep.makeRequest();
        Request expectedRequest = MockXacmlRequest.getRequestWithSubjectAttributes();

        assertThat(request, is(expectedRequest));
    }

    @Test(expected = PepException.class)
    public void requestThrowsExceptionNonValidValues() {
        pep.withClientValues(MockXacmlRequest.OIDC_TOKEN, MockXacmlRequest.SUBJECT_ID, null, MockXacmlRequest.FNR, MockXacmlRequest.CREDENTIAL_RESOURCE);
        pep.makeRequest();
    }
}