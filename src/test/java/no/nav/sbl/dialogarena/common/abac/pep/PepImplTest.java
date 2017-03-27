package no.nav.sbl.dialogarena.common.abac.pep;

import no.nav.brukerdialog.security.context.SubjectHandlerUtils;
import no.nav.brukerdialog.security.context.ThreadLocalSubjectHandler;
import no.nav.brukerdialog.security.domain.IdentType;
import no.nav.sbl.dialogarena.common.abac.pep.domain.request.XacmlRequest;
import no.nav.sbl.dialogarena.common.abac.pep.domain.response.*;
import no.nav.sbl.dialogarena.common.abac.pep.exception.AbacException;
import no.nav.sbl.dialogarena.common.abac.pep.exception.PepException;
import no.nav.sbl.dialogarena.common.abac.pep.service.AbacService;
import no.nav.sbl.dialogarena.common.abac.pep.service.LdapService;
import org.junit.*;
import org.mockito.*;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import static java.lang.System.setProperty;
import static org.hamcrest.CoreMatchers.is;
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