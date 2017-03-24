package no.nav.sbl.dialogarena.common.abac.pep;

import no.nav.abac.xacml.NavAttributter;
import no.nav.abac.xacml.StandardAttributter;
import no.nav.sbl.dialogarena.common.abac.pep.domain.Attribute;
import no.nav.sbl.dialogarena.common.abac.pep.domain.ResourceType;
import no.nav.sbl.dialogarena.common.abac.pep.domain.request.*;
import no.nav.sbl.dialogarena.common.abac.pep.exception.PepException;
import org.junit.Before;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

public class XacmlRequestGeneratorTest {

    private XacmlRequestGenerator xacmlRequestGenerator;
    private Client client;

    @Before
    public void setup() throws PepException {
        client = new Client(MockXacmlRequest.OIDC_TOKEN, MockXacmlRequest.SUBJECT_ID, MockXacmlRequest.FNR, ResourceType.Person, MockXacmlRequest.DOMAIN, MockXacmlRequest.CREDENTIAL_RESOURCE)
                .withCredentialResource(MockXacmlRequest.CREDENTIAL_RESOURCE);

        xacmlRequestGenerator = new XacmlRequestGenerator(client);
    }

    @Test
    public void buildsCorrectEnvironmentWithOidcToken() {
        Environment environment = xacmlRequestGenerator.makeEnvironment();
        List<Attribute> expectedAttributes = new ArrayList<>();
        expectedAttributes.add(new Attribute(NavAttributter.ENVIRONMENT_FELLES_OIDC_TOKEN_BODY, MockXacmlRequest.OIDC_TOKEN));
        expectedAttributes.add(new Attribute(NavAttributter.ENVIRONMENT_FELLES_PEP_ID, MockXacmlRequest.CREDENTIAL_RESOURCE));

        assertThat(environment.getAttribute(), is(expectedAttributes));
    }

    @Test
    public void buildsCorrectEnvironmentWithoutOidcToken() throws PepException {
        client.setOidcToken(null);

        xacmlRequestGenerator = new XacmlRequestGenerator(client);

        Environment environment = xacmlRequestGenerator.makeEnvironment();
        List<Attribute> expectedAttributes = new ArrayList<>();
        expectedAttributes.add(new Attribute(NavAttributter.ENVIRONMENT_FELLES_PEP_ID, MockXacmlRequest.CREDENTIAL_RESOURCE));

        assertThat(environment.getAttribute(), is(expectedAttributes));
    }

    @Test
    public void buildsCorrectResourceWithTypePerson() {
        Resource resource = xacmlRequestGenerator.makeResource(ResourceType.Person);
        List<Attribute> expectedAttributes = new ArrayList<>();
        expectedAttributes.add(new Attribute(NavAttributter.RESOURCE_FELLES_RESOURCE_TYPE, NavAttributter.RESOURCE_FELLES_PERSON));
        expectedAttributes.add(new Attribute(NavAttributter.RESOURCE_FELLES_DOMENE, MockXacmlRequest.DOMAIN));
        expectedAttributes.add(new Attribute(NavAttributter.RESOURCE_FELLES_PERSON_FNR, MockXacmlRequest.FNR));

        assertThat(resource.getAttribute(), is(expectedAttributes));
    }

    @Test
    public void buildsCorrectResourceWithTypeEgenAnsatt() {
        Resource resource = xacmlRequestGenerator.makeResource(ResourceType.EgenAnsatt);
        List<Attribute> expectedAttributes = new ArrayList<>();
        expectedAttributes.add(new Attribute(NavAttributter.RESOURCE_FELLES_RESOURCE_TYPE, NavAttributter.SUBJECT_FELLES_HAR_TILGANG_EGEN_ANSATT));
        expectedAttributes.add(new Attribute(NavAttributter.RESOURCE_FELLES_DOMENE, MockXacmlRequest.DOMAIN));

        assertThat(resource.getAttribute(), is(expectedAttributes));
    }

    @Test
    public void buildsCorrectResourceWithTypeKode6() {
        Resource resource = xacmlRequestGenerator.makeResource(ResourceType.Kode6);
        List<Attribute> expectedAttributes = new ArrayList<>();
        expectedAttributes.add(new Attribute(NavAttributter.RESOURCE_FELLES_RESOURCE_TYPE, NavAttributter.SUBJECT_FELLES_HAR_TILGANG_KODE_6));
        expectedAttributes.add(new Attribute(NavAttributter.RESOURCE_FELLES_DOMENE, MockXacmlRequest.DOMAIN));

        assertThat(resource.getAttribute(), is(expectedAttributes));
    }

    @Test
    public void buildsCorrectResourceWithTypeKode7() {
        Resource resource = xacmlRequestGenerator.makeResource(ResourceType.Kode7);
        List<Attribute> expectedAttributes = new ArrayList<>();
        expectedAttributes.add(new Attribute(NavAttributter.RESOURCE_FELLES_RESOURCE_TYPE, NavAttributter.SUBJECT_FELLES_HAR_TILGANG_KODE_7));
        expectedAttributes.add(new Attribute(NavAttributter.RESOURCE_FELLES_DOMENE, MockXacmlRequest.DOMAIN));

        assertThat(resource.getAttribute(), is(expectedAttributes));
    }

    @Test
    public void buildsCorrectAccessSubject() {
        AccessSubject accessSubject = xacmlRequestGenerator.makeAccessSubject();
        List<Attribute> expectedAttributes = new ArrayList<>();
        expectedAttributes.add(new Attribute(StandardAttributter.SUBJECT_ID, MockXacmlRequest.SUBJECT_ID));
        expectedAttributes.add(new Attribute(NavAttributter.SUBJECT_FELLES_SUBJECTTYPE, "InternBruker"));

        assertThat(accessSubject.getAttribute(), is(expectedAttributes));
    }

    @Test
    public void buildsCorrectAction() {
        Action action = xacmlRequestGenerator.makeAction();
        List<Attribute> expectedAttributes = new ArrayList<>();
        expectedAttributes.add(new Attribute(StandardAttributter.ACTION_ID, "read"));

        assertThat(action.getAttribute(), is(expectedAttributes));
    }

    @Test
    public void buildsCorrectRequestWithOidcToken() throws PepException {
        client.setSubjectId(null);
        xacmlRequestGenerator = new XacmlRequestGenerator(client);

        Request request = xacmlRequestGenerator.makeRequest(ResourceType.Person);
        Request expectedRequest = MockXacmlRequest.getRequest();

        assertThat(request, is(expectedRequest));
    }

    @Test
    public void buildsCorrectRequestWithSubjectId() throws PepException {
        client.setOidcToken(null);
        xacmlRequestGenerator = new XacmlRequestGenerator(client);

        Request request = xacmlRequestGenerator.makeRequest(ResourceType.Person);
        Request expectedRequest = MockXacmlRequest.getRequestWithSubjectAttributes();

        assertThat(request, is(expectedRequest));
    }

    @Test(expected = PepException.class)
    public void requestThrowsExceptionNonValidValues() throws PepException {
        client.withOidcToken(null).withSubjectId(null);
        xacmlRequestGenerator = new XacmlRequestGenerator(client);
        xacmlRequestGenerator.makeRequest(ResourceType.Person);
    }
}