package no.nav.sbl.dialogarena.common.abac.pep;

import no.nav.abac.xacml.NavAttributter;
import no.nav.abac.xacml.StandardAttributter;
import no.nav.sbl.dialogarena.common.abac.pep.domain.Attribute;
import no.nav.sbl.dialogarena.common.abac.pep.domain.request.*;
import no.nav.sbl.dialogarena.common.abac.pep.domain.response.*;

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
        when(pdpService.askForPermission(any(XacmlRequest.class)))
                .thenReturn(getMockResponse(Decision.Permit));

        final BiasedDecisionResponse biasedDecisionResponse = pep.evaluateWithBias(MockXacmlRequest.OIDC_TOKEN,
                MockXacmlRequest.SUBJECT_ID, MockXacmlRequest.DOMAIN, MockXacmlRequest.FNR, MockXacmlRequest.CREDENTIAL_RESOURCE);

        assertThat(biasedDecisionResponse.getBiasedDecision(), is(Decision.Permit));
    }

    @Test
    public void returnsDenyForNotApplicable() {
        when(pdpService.askForPermission(any(XacmlRequest.class)))
                .thenReturn(getMockResponse(Decision.NotApplicable));

        final BiasedDecisionResponse biasedDecisionResponse = pep.evaluateWithBias(MockXacmlRequest.OIDC_TOKEN, MockXacmlRequest.SUBJECT_ID,
                MockXacmlRequest.DOMAIN, MockXacmlRequest.FNR, MockXacmlRequest.CREDENTIAL_RESOURCE);

        assertThat(biasedDecisionResponse.getBiasedDecision(), is(Decision.Deny));
    }

    @Test(expected = PepException.class)
    public void decisionIndeterminateThrowsException() {
        when(pdpService.askForPermission(any(XacmlRequest.class)))
                .thenReturn(getMockResponse(Decision.Indeterminate));

        final BiasedDecisionResponse biasedDecisionResponse = pep.evaluateWithBias(MockXacmlRequest.OIDC_TOKEN, MockXacmlRequest.SUBJECT_ID,
                MockXacmlRequest.DOMAIN, MockXacmlRequest.FNR, MockXacmlRequest.CREDENTIAL_RESOURCE);

        assertThat(biasedDecisionResponse.getBiasedDecision(), is(Decision.Deny));
    }

    @Test
    public void buildsCorrectEnvironmentWithOidcToken() {
        Environment environment = pep.makeEnvironment();
        List<Attribute> expectedAttributes = new ArrayList<>();
        expectedAttributes.add(new Attribute(NavAttributter.ENVIRONMENT_FELLES_OIDC_TOKEN_BODY, MockXacmlRequest.OIDC_TOKEN));
        expectedAttributes.add(new Attribute(NavAttributter.ENVIRONMENT_FELLES_PEP_ID, MockXacmlRequest.CREDENTIAL_RESOURCE));

        assertThat(environment.getAttribute(), is(expectedAttributes));
    }

    @Test
    public void buildsCorrectEnvironmentWithoutOidcToken() {
        pep.withClientValues(null, MockXacmlRequest.SUBJECT_ID, MockXacmlRequest.DOMAIN, MockXacmlRequest.FNR, MockXacmlRequest.CREDENTIAL_RESOURCE);
        Environment environment = pep.makeEnvironment();
        List<Attribute> expectedAttributes = new ArrayList<>();
        expectedAttributes.add(new Attribute(NavAttributter.ENVIRONMENT_FELLES_PEP_ID, MockXacmlRequest.CREDENTIAL_RESOURCE));

        assertThat(environment.getAttribute(), is(expectedAttributes));
    }

    @Test
    public void buildsCorrectResource() {
        Resource resource = pep.makeResource();
        List<Attribute> expectedAttributes = new ArrayList<>();
        expectedAttributes.add(new Attribute(NavAttributter.RESOURCE_FELLES_RESOURCE_TYPE, NavAttributter.RESOURCE_FELLES_PERSON));
        expectedAttributes.add(new Attribute(NavAttributter.RESOURCE_FELLES_DOMENE, MockXacmlRequest.DOMAIN));
        expectedAttributes.add(new Attribute(NavAttributter.RESOURCE_FELLES_PERSON_FNR, MockXacmlRequest.FNR));

        assertThat(resource.getAttribute(), is(expectedAttributes));
    }

    @Test
    public void buildsCorrectAccessSubject() {
        AccessSubject accessSubject = pep.makeAccessSubject();
        List<Attribute> expectedAttributes = new ArrayList<>();
        expectedAttributes.add(new Attribute(StandardAttributter.SUBJECT_ID, MockXacmlRequest.SUBJECT_ID));
        expectedAttributes.add(new Attribute(NavAttributter.SUBJECT_FELLES_SUBJECTTYPE, "InternBruker"));

        assertThat(accessSubject.getAttribute(), is(expectedAttributes));
    }

    @Test
    public void buildsCorrectAction() {
        Action action = pep.makeAction();
        List<Attribute> expectedAttributes = new ArrayList<>();
        expectedAttributes.add(new Attribute(StandardAttributter.ACTION_ID, "read"));

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
        pep.withClientValues(MockXacmlRequest.OIDC_TOKEN, null, null, MockXacmlRequest.FNR, MockXacmlRequest.CREDENTIAL_RESOURCE);
        pep.makeRequest();
    }

    private XacmlResponse getMockResponse(Decision decision) {
        List<Response> responses = new ArrayList<>();
        responses.add(new Response().withDecision(decision));
        return new XacmlResponse().withResponse(responses);
    }
}