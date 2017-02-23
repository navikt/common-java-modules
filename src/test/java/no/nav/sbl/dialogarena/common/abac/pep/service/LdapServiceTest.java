package no.nav.sbl.dialogarena.common.abac.pep.service;

import no.nav.abac.xacml.StandardAttributter;
import no.nav.sbl.dialogarena.common.abac.pep.MockXacmlRequest;
import no.nav.sbl.dialogarena.common.abac.pep.domain.Attribute;
import no.nav.sbl.dialogarena.common.abac.pep.domain.request.AccessSubject;
import no.nav.sbl.dialogarena.common.abac.pep.domain.request.XacmlRequest;
import no.nav.sbl.dialogarena.common.abac.pep.domain.response.Decision;
import no.nav.sbl.dialogarena.common.abac.pep.domain.response.XacmlResponse;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import javax.naming.NamingException;
import javax.naming.directory.*;

import static java.lang.System.setProperty;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class LdapServiceTest {

    private static final String AD_ROLE = "0000-GA-Modia-Oppfolging";

    @Mock
    Ldap ldap;

    @InjectMocks
    LdapService ldapService;

    @BeforeClass
    public static void setUp() throws Exception {
        setProperty("ldap.url", "www.something.com");
        setProperty("ldap.username", "username");
        setProperty("ldap.password", "supersecrectpassword");
        setProperty("role", AD_ROLE);
    }

    @Test
    public void missingRoleGivesDeny() {

        when(ldap.getAttributes(anyString())).thenReturn(mockLdapUtenRiktigRolle());

        final XacmlResponse xacmlResponse = ldapService.askForPermission(getRequest());

        assertThat(xacmlResponse.getResponse().get(0).getDecision(), is(Decision.Deny));

    }

    @Test
    public void havingRoleGivesPermit() throws NamingException {

        when(ldap.getAttributes(anyString())).thenReturn(mockLdapMedRiktigRolle());

        final XacmlResponse xacmlResponse = ldapService.askForPermission(getRequest());

        assertThat(xacmlResponse.getResponse().get(0).getDecision(), is(Decision.Permit));

    }

    private XacmlRequest getRequest() {
        final XacmlRequest xacmlRequest = MockXacmlRequest.getXacmlRequest();
        final AccessSubject accessSubject = new AccessSubject();
        accessSubject.getAttribute().add(new Attribute(StandardAttributter.SUBJECT_ID, "Z999999"));
        xacmlRequest.getRequest().withAccessSubject(accessSubject);
        return xacmlRequest;
    }

    private Attributes mockLdapMedRiktigRolle() throws NamingException {
        final Attributes attributes = mockLdapUtenRiktigRolle();
        BasicAttribute attribute = (BasicAttribute) attributes.get(LdapService.WANTED_ATTRIBUTE).get();
        attribute.add("CN=" + AD_ROLE + ",OU=AccountGroups,OU=Groups,OU=NAV,OU=BusinessUnits,DC=test,DC=local");
        return attributes;
    }

    private Attributes mockLdapUtenRiktigRolle() {
        final Attributes attributes = new BasicAttributes();
        final BasicAttribute attribute = new BasicAttribute(LdapService.WANTED_ATTRIBUTE);
        attribute.add("CN=0000-GA-Modia-Test1,OU=AccountGroups,OU=Groups,OU=NAV,OU=BusinessUnits,DC=test,DC=local");
        attribute.add("CN=0000-GA-Modia-Test2,OU=AccountGroups,OU=Groups,OU=NAV,OU=BusinessUnits,DC=test,DC=local");
        attribute.add("CN=0000-GA-Modia-Test3,OU=AccountGroups,OU=Groups,OU=NAV,OU=BusinessUnits,DC=test,DC=local");
        attributes.put(LdapService.WANTED_ATTRIBUTE, attribute);
        return attributes;
    }


}