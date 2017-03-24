package no.nav.sbl.dialogarena.common.abac.pep;

import no.nav.abac.xacml.NavAttributter;
import no.nav.abac.xacml.StandardAttributter;
import no.nav.brukerdialog.security.context.SubjectHandlerUtils;
import no.nav.brukerdialog.security.context.ThreadLocalSubjectHandler;
import no.nav.brukerdialog.security.domain.IdentType;
import no.nav.sbl.dialogarena.common.abac.pep.domain.Attribute;
import no.nav.sbl.dialogarena.common.abac.pep.domain.ResourceType;
import no.nav.sbl.dialogarena.common.abac.pep.domain.request.*;
import no.nav.sbl.dialogarena.common.abac.pep.domain.response.*;
import no.nav.sbl.dialogarena.common.abac.pep.exception.AbacException;
import no.nav.sbl.dialogarena.common.abac.pep.exception.PepException;
import no.nav.sbl.dialogarena.common.abac.pep.service.AbacService;
import no.nav.sbl.dialogarena.common.abac.pep.service.LdapService;
import org.junit.*;
import org.mockito.*;

import java.io.IOException;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;

import static java.lang.System.setProperty;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

public class PepImplTest {

    @InjectMocks
    PepImpl pep;

    @Mock
    LdapService ldapService;
    @Mock
    AbacService abacService;

    @BeforeClass
    public static void setUp() throws Exception {
        setProperty(CredentialConstants.SYSTEMUSER_USERNAME, "username");
        setProperty(CredentialConstants.SYSTEMUSER_PASSWORD, "password");
        setProperty("no.nav.modig.security.systemuser.username", "username");
        setProperty("no.nav.modig.security.systemuser.password", "password");
        setProperty("no.nav.modig.core.context.subjectHandlerImplementationClass", ThreadLocalSubjectHandler.class.getName());
        SubjectHandlerUtils.setSubject(new SubjectHandlerUtils.SubjectBuilder("userId", IdentType.InternBruker).withAuthLevel(3).getSubject());
    }

    @Before
    public void setup() {
        MockitoAnnotations.initMocks(this);
        pep.withClientValues(MockXacmlRequest.OIDC_TOKEN, MockXacmlRequest.SUBJECT_ID, MockXacmlRequest.DOMAIN, MockXacmlRequest.FNR, MockXacmlRequest.CREDENTIAL_RESOURCE);
    }

    @Test
    public void returnsDecision() throws AbacException, IOException, NoSuchFieldException, PepException {
        when(abacService.askForPermission(any(XacmlRequest.class)))
                .thenReturn(getMockResponse(Decision.Permit));

        final BiasedDecisionResponse biasedDecisionResponse = pep.isServiceCallAllowedWithIdent(
                MockXacmlRequest.SUBJECT_ID, MockXacmlRequest.DOMAIN, MockXacmlRequest.FNR);

        assertThat(biasedDecisionResponse.getBiasedDecision(), is(Decision.Permit));
    }

    @Test
    public void returnsDenyForNotApplicable() throws AbacException, IOException, NoSuchFieldException, PepException {
        when(abacService.askForPermission(any(XacmlRequest.class)))
                .thenReturn(getMockResponse(Decision.NotApplicable));

        final BiasedDecisionResponse biasedDecisionResponse = pep.isServiceCallAllowedWithIdent(
                MockXacmlRequest.SUBJECT_ID, MockXacmlRequest.DOMAIN, MockXacmlRequest.FNR);

        assertThat(biasedDecisionResponse.getBiasedDecision(), is(Decision.Deny));
    }

    @Test(expected = PepException.class)
    public void decisionIndeterminateThrowsException() throws AbacException, IOException, NoSuchFieldException, PepException {
        when(abacService.askForPermission(any(XacmlRequest.class)))
                .thenReturn(getMockResponse(Decision.Indeterminate));

        final BiasedDecisionResponse biasedDecisionResponse = pep.isServiceCallAllowedWithIdent(
                MockXacmlRequest.SUBJECT_ID, MockXacmlRequest.DOMAIN, MockXacmlRequest.FNR);

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
    public void buildsCorrectResourceWithTypePerson() {
        Resource resource = pep.makeResource(ResourceType.Person);
        List<Attribute> expectedAttributes = new ArrayList<>();
        expectedAttributes.add(new Attribute(NavAttributter.RESOURCE_FELLES_RESOURCE_TYPE, NavAttributter.RESOURCE_FELLES_PERSON));
        expectedAttributes.add(new Attribute(NavAttributter.RESOURCE_FELLES_DOMENE, MockXacmlRequest.DOMAIN));
        expectedAttributes.add(new Attribute(NavAttributter.RESOURCE_FELLES_PERSON_FNR, MockXacmlRequest.FNR));

        assertThat(resource.getAttribute(), is(expectedAttributes));
    }

    @Test
    public void buildsCorrectResourceWithTypeEgenAnsatt() {
        Resource resource = pep.makeResource(ResourceType.EgenAnsatt);
        List<Attribute> expectedAttributes = new ArrayList<>();
        expectedAttributes.add(new Attribute(NavAttributter.RESOURCE_FELLES_RESOURCE_TYPE, NavAttributter.SUBJECT_FELLES_HAR_TILGANG_EGEN_ANSATT));
        expectedAttributes.add(new Attribute(NavAttributter.RESOURCE_FELLES_DOMENE, MockXacmlRequest.DOMAIN));

        assertThat(resource.getAttribute(), is(expectedAttributes));
    }

    @Test
    public void buildsCorrectResourceWithTypeKode6() {
        Resource resource = pep.makeResource(ResourceType.Kode6);
        List<Attribute> expectedAttributes = new ArrayList<>();
        expectedAttributes.add(new Attribute(NavAttributter.RESOURCE_FELLES_RESOURCE_TYPE, NavAttributter.SUBJECT_FELLES_HAR_TILGANG_KODE_6));
        expectedAttributes.add(new Attribute(NavAttributter.RESOURCE_FELLES_DOMENE, MockXacmlRequest.DOMAIN));

        assertThat(resource.getAttribute(), is(expectedAttributes));
    }

    @Test
    public void buildsCorrectResourceWithTypeKode7() {
        Resource resource = pep.makeResource(ResourceType.Kode7);
        List<Attribute> expectedAttributes = new ArrayList<>();
        expectedAttributes.add(new Attribute(NavAttributter.RESOURCE_FELLES_RESOURCE_TYPE, NavAttributter.SUBJECT_FELLES_HAR_TILGANG_KODE_7));
        expectedAttributes.add(new Attribute(NavAttributter.RESOURCE_FELLES_DOMENE, MockXacmlRequest.DOMAIN));

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
    public void buildsCorrectRequestWithOidcToken() throws PepException {
        pep.withClientValues(MockXacmlRequest.OIDC_TOKEN, null, MockXacmlRequest.DOMAIN, MockXacmlRequest.FNR, MockXacmlRequest.CREDENTIAL_RESOURCE);
        Request request = pep.makeRequest(ResourceType.Person);
        Request expectedRequest = MockXacmlRequest.getRequest();

        assertThat(request, is(expectedRequest));
    }

    @Test
    public void buildsCorrectRequestWithSubjectId() throws PepException {
        pep.withClientValues(null, MockXacmlRequest.SUBJECT_ID, MockXacmlRequest.DOMAIN, MockXacmlRequest.FNR, MockXacmlRequest.CREDENTIAL_RESOURCE);
        Request request = pep.makeRequest(ResourceType.Person);
        Request expectedRequest = MockXacmlRequest.getRequestWithSubjectAttributes();

        assertThat(request, is(expectedRequest));
    }

    @Test(expected = PepException.class)
    public void requestThrowsExceptionNonValidValues() throws PepException {
        pep.withClientValues(MockXacmlRequest.OIDC_TOKEN, null, null, MockXacmlRequest.FNR, MockXacmlRequest.CREDENTIAL_RESOURCE);
        pep.makeRequest(ResourceType.Person);
    }

    @Test
    public void clientValuesAreReset() throws NoSuchFieldException, AbacException, IllegalAccessException, IOException, PepException {
        when(abacService.askForPermission(any(XacmlRequest.class)))
                .thenReturn(getMockResponse(Decision.Permit));

        pep.isServiceCallAllowedWithIdent("Z999000", "veilarb", "10108000398");
        pep.isServiceCallAllowedWithOidcToken("token", "veilarb", "10108000398");

        final Field client = PepImpl.class.getDeclaredField("client");
        client.setAccessible(true);
        final Client c = (Client) client.get(pep);

        assertThat(c.getSubjectId(), is(nullValue()));
        assertThat(c.getOidcToken(), is("token"));
    }

    @Test(expected = IllegalArgumentException.class)
    public void nullFnrThrowsIllegalArgumentException() throws PepException {
        pep.isServiceCallAllowedWithIdent("Z999000", "veilarb", null);
    }

    @Test(expected = IllegalArgumentException.class)
    public void notNumericFnrThrowsIllegalArgumentException() throws PepException {
        pep.isServiceCallAllowedWithIdent("Z999000", "veilarb", "xxxxxx4444");
    }

    @Test(expected = IllegalArgumentException.class)
    public void wrongLengthOfFnrThrowsIllegalArgumentException() throws PepException {
        pep.isServiceCallAllowedWithIdent("Z999000", "veilarb", "xxxx4444");
    }

    private XacmlResponse getMockResponse(Decision decision) {
        List<Response> responses = new ArrayList<>();
        responses.add(new Response().withDecision(decision));
        return new XacmlResponse().withResponse(responses);
    }
}