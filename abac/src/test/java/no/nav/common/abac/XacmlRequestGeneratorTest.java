package no.nav.common.abac;

import no.nav.common.abac.domain.Attribute;
import no.nav.common.abac.domain.ResourceType;
import no.nav.common.abac.domain.request.*;
import no.nav.common.abac.exception.PepException;
import no.nav.common.utils.EnvironmentUtils;
import org.junit.Before;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

import static no.nav.common.abac.TestUtils.assertJson;
import static no.nav.common.abac.TestUtils.getContentFromJsonFile;
import static no.nav.common.test.SystemProperties.setTemporaryProperty;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

public class XacmlRequestGeneratorTest {

    private XacmlRequestGenerator xacmlRequestGenerator = new XacmlRequestGenerator();
    private RequestData requestData;

    @Before
    public void setup() throws PepException {
        requestData = new RequestData()
                .withOidcToken(MockXacmlRequest.OIDC_TOKEN)
                .withSamlToken(MockXacmlRequest.SAML_TOKEN)
                .withSubjectId(MockXacmlRequest.SUBJECT_ID)
                .withPersonId(MockXacmlRequest.FNR)
                .withResourceType(ResourceType.Person)
                .withDomain(MockXacmlRequest.DOMAIN)
                .withCredentialResource(MockXacmlRequest.CREDENTIAL_RESOURCE);
    }

    @Test
    public void buildsCorrectEnvironment() {
        Environment environment = xacmlRequestGenerator.makeEnvironment(requestData);
        List<Attribute> expectedAttributes = new ArrayList<>();
        expectedAttributes.add(new Attribute(NavAttributter.ENVIRONMENT_FELLES_OIDC_TOKEN_BODY, MockXacmlRequest.OIDC_TOKEN));
        expectedAttributes.add(new Attribute(NavAttributter.ENVIRONMENT_FELLES_SAML_TOKEN, MockXacmlRequest.SAML_TOKEN));
        expectedAttributes.add(new Attribute(NavAttributter.ENVIRONMENT_FELLES_PEP_ID, MockXacmlRequest.CREDENTIAL_RESOURCE));

        assertThat(environment.getAttribute(), is(expectedAttributes));
    }

    @Test
    public void buildsCorrectEnvironmentWithoutSAMLToken() throws PepException {
        Environment environment = xacmlRequestGenerator.makeEnvironment(requestData.withSamlToken(null));
        List<Attribute> expectedAttributes = new ArrayList<>();
        expectedAttributes.add(new Attribute(NavAttributter.ENVIRONMENT_FELLES_OIDC_TOKEN_BODY, MockXacmlRequest.OIDC_TOKEN));
        expectedAttributes.add(new Attribute(NavAttributter.ENVIRONMENT_FELLES_PEP_ID, MockXacmlRequest.CREDENTIAL_RESOURCE));

        assertThat(environment.getAttribute(), is(expectedAttributes));
    }

    @Test
    public void buildsCorrectEnvironmentWithoutOidcToken() throws PepException {
        Environment environment = xacmlRequestGenerator.makeEnvironment(requestData.withOidcToken(null));
        List<Attribute> expectedAttributes = new ArrayList<>();
        expectedAttributes.add(new Attribute(NavAttributter.ENVIRONMENT_FELLES_SAML_TOKEN, MockXacmlRequest.SAML_TOKEN));
        expectedAttributes.add(new Attribute(NavAttributter.ENVIRONMENT_FELLES_PEP_ID, MockXacmlRequest.CREDENTIAL_RESOURCE));

        assertThat(environment.getAttribute(), is(expectedAttributes));
    }

    @Test
    public void buildsCorrectResourceWithTypePerson() {
        Resource resource = xacmlRequestGenerator.makeResource(requestData);
        List<Attribute> expectedAttributes = new ArrayList<>();
        expectedAttributes.add(new Attribute(NavAttributter.RESOURCE_FELLES_RESOURCE_TYPE, NavAttributter.RESOURCE_FELLES_PERSON));
        expectedAttributes.add(new Attribute(NavAttributter.RESOURCE_FELLES_DOMENE, MockXacmlRequest.DOMAIN));
        expectedAttributes.add(new Attribute(NavAttributter.RESOURCE_FELLES_PERSON_FNR, MockXacmlRequest.FNR.getId()));

        assertThat(resource.getAttribute(), is(expectedAttributes));
    }

    @Test
    public void buildsCorrectResourceWithTypeEgenAnsatt() {
        Resource resource = xacmlRequestGenerator.makeResource(requestData.withResourceType(ResourceType.EgenAnsatt));
        List<Attribute> expectedAttributes = new ArrayList<>();
        expectedAttributes.add(new Attribute(NavAttributter.RESOURCE_FELLES_RESOURCE_TYPE, NavAttributter.SUBJECT_FELLES_HAR_TILGANG_EGEN_ANSATT));
        expectedAttributes.add(new Attribute(NavAttributter.RESOURCE_FELLES_DOMENE, MockXacmlRequest.DOMAIN));

        assertThat(resource.getAttribute(), is(expectedAttributes));
    }

    @Test
    public void buildsCorrectResourceWithTypeKode6() {
        Resource resource = xacmlRequestGenerator.makeResource(requestData.withResourceType(ResourceType.Kode6));
        List<Attribute> expectedAttributes = new ArrayList<>();
        expectedAttributes.add(new Attribute(NavAttributter.RESOURCE_FELLES_RESOURCE_TYPE, NavAttributter.SUBJECT_FELLES_HAR_TILGANG_KODE_6));
        expectedAttributes.add(new Attribute(NavAttributter.RESOURCE_FELLES_DOMENE, MockXacmlRequest.DOMAIN));

        assertThat(resource.getAttribute(), is(expectedAttributes));
    }

    @Test
    public void buildsCorrectResourceWithTypeKode7() {
        Resource resource = xacmlRequestGenerator.makeResource(requestData.withResourceType(ResourceType.Kode7));
        List<Attribute> expectedAttributes = new ArrayList<>();
        expectedAttributes.add(new Attribute(NavAttributter.RESOURCE_FELLES_RESOURCE_TYPE, NavAttributter.SUBJECT_FELLES_HAR_TILGANG_KODE_7));
        expectedAttributes.add(new Attribute(NavAttributter.RESOURCE_FELLES_DOMENE, MockXacmlRequest.DOMAIN));

        assertThat(resource.getAttribute(), is(expectedAttributes));
    }

    @Test
    public void buildsCorrectAccessSubject() {
        AccessSubject accessSubject = xacmlRequestGenerator.makeAccessSubject(requestData);
        List<Attribute> expectedAttributes = new ArrayList<>();
        expectedAttributes.add(new Attribute(StandardAttributter.SUBJECT_ID, MockXacmlRequest.SUBJECT_ID));
        expectedAttributes.add(new Attribute(NavAttributter.SUBJECT_FELLES_SUBJECTTYPE, "InternBruker"));

        assertThat(accessSubject.getAttribute(), is(expectedAttributes));
    }

    @Test
    public void buildsCorrectAction() {
        Action action = xacmlRequestGenerator.makeAction(requestData);
        List<Attribute> expectedAttributes = new ArrayList<>();
        expectedAttributes.add(new Attribute(StandardAttributter.ACTION_ID, "read"));

        assertThat(action.getAttribute(), is(expectedAttributes));
    }

    @Test
    public void buildsCorrectRequestWithOidcToken() throws PepException {
        Request request = xacmlRequestGenerator.makeRequest(requestData.withSubjectId(null).withSamlToken(null));
        Request expectedRequest = MockXacmlRequest.getRequest();

        assertThat(request, is(expectedRequest));
    }

    @Test
    public void buildsCorrectRequestWithSAMLToken() throws PepException {
        Request request = xacmlRequestGenerator.makeRequest(requestData.withSubjectId(null).withOidcToken(null));
        Request expectedRequest = MockXacmlRequest.getSAMLRequest();

        assertThat(request, is(expectedRequest));
    }

    @Test
    public void buildsCorrectRequestWithSubjectId() throws PepException {
        Request request = xacmlRequestGenerator.makeRequest(requestData.withOidcToken(null).withSamlToken(null));
        Request expectedRequest = MockXacmlRequest.getRequestWithSubjectAttributes();

        assertThat(request, is(expectedRequest));
    }

    @Test
    public void buildsCorrectPingRequest() {
        setTemporaryProperty(EnvironmentUtils.APP_NAME_PROPERTY_NAME, "testapp", () -> {
            XacmlRequest pingRequest = XacmlRequestGenerator.getPingRequest();
            final String stringEntity = XacmlMapper.mapRequestToEntity(pingRequest);

            String expectedContent = getContentFromJsonFile("xacmlrequest-ping.json");
            assertJson(stringEntity, expectedContent);
        });
    }

    @Test(expected = PepException.class)
    public void requestThrowsExceptionNonValidValues() throws PepException {
        xacmlRequestGenerator.makeRequest(requestData.withOidcToken(null).withSubjectId(null).withSamlToken(null));
    }

    @Test
    public void requestThrowsExceptionMaskedValues() {
        assertThatThrownBy(()-> xacmlRequestGenerator.makeRequest(requestData.withOidcToken("oidc-secret-token").withSamlToken("saml-secret-token").withDomain(null)))
                .hasMessageContaining("oidc-sec*********")
                .hasMessageContaining("saml-sec*********");
    }

}
