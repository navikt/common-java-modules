package no.nav.sbl.dialogarena.common.abac.pep.service;

import mockit.Expectations;
import mockit.Tested;
import no.nav.sbl.dialogarena.common.abac.pep.MockXacmlRequest;
import no.nav.sbl.dialogarena.common.abac.pep.domain.response.*;
import no.nav.sbl.dialogarena.common.abac.pep.exception.AbacException;
import org.apache.http.HttpStatus;
import org.apache.http.client.methods.HttpPost;
import org.junit.BeforeClass;
import org.junit.Test;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import static java.lang.System.setProperty;
import static no.nav.sbl.dialogarena.common.abac.TestUtils.getContentFromJsonFile;
import static no.nav.sbl.dialogarena.common.abac.TestUtils.prepareResponse;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsEqual.equalTo;

public class AbacServiceTest {

    @Tested
    AbacService abacService;

    @BeforeClass
    public static void setup() {
        setProperty("ldap.url", "www.something.com");
        setProperty("ldap.username", "username");
        setProperty("ldap.password", "supersecrectpassword");
        setProperty("abac.endpoint.url", "www.abac.com");
    }

    @Test
    public void returnsResponse() throws IOException, AbacException, NoSuchFieldException {

        new Expectations(AbacService.class) {{
            abacService.doPost(withAny(new HttpPost()));
            result = prepareResponse(200, getContentFromJsonFile("xacmlresponse.json"));
        }};

        final XacmlResponse actualXacmlResponse = abacService.askForPermission(MockXacmlRequest.getXacmlRequest());


        final XacmlResponse expectedXacmlResponse = getExpectedXacmlResponse();

        assertThat(actualXacmlResponse, is(equalTo(expectedXacmlResponse)));

    }

    @Test(expected = AbacException.class)
    public void throwsExceptionAtFailureAgainstABAC() throws IOException, AbacException, NoSuchFieldException {

        new Expectations(AbacService.class) {{
            abacService.doPost(withAny(new HttpPost()));
            result = prepareResponse(HttpStatus.SC_INTERNAL_SERVER_ERROR, "");
        }};

        abacService.askForPermission(MockXacmlRequest.getXacmlRequest());

    }

    private XacmlResponse getExpectedXacmlResponse() {

        final List<AttributeAssignment> attributeAssignments = new ArrayList<>();
        attributeAssignments.add(new AttributeAssignment("no.nav.abac.advice.fritekst", "Ikke tilgang"));

        final Advice advice = new Advice("no.nav.abac.advices.reason.deny_reason", attributeAssignments);

        final List<Advice> associatedAdvice = new ArrayList<>();
        associatedAdvice.add(advice);

        final Response response = new Response()
                .withDecision(Decision.Deny)
                .withAssociatedAdvice(associatedAdvice);
        final List<Response> responses = new ArrayList<>();
        responses.add(response);

        return new XacmlResponse()
                .withResponse(responses);
    }


}

