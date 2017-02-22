package no.nav.sbl.dialogarena.common.abac.pep.service;

import no.nav.abac.xacml.StandardAttributter;
import no.nav.sbl.dialogarena.common.abac.pep.domain.request.XacmlRequest;
import no.nav.sbl.dialogarena.common.abac.pep.domain.response.*;

import javax.naming.*;
import javax.naming.directory.*;
import javax.naming.ldap.InitialLdapContext;
import javax.naming.ldap.LdapContext;
import java.util.*;

import static java.lang.System.getProperty;

//må bruke Hashtable i InitiallLdapContext dessverre.
@SuppressWarnings({"squid:S1149"})
public class LdapService implements TilgangService {

    private static final String WANTED_ATTRIBUTE = "memberof";
    private static final Hashtable<String, String> env = new Hashtable<>();

    static {
        env.put(Context.INITIAL_CONTEXT_FACTORY, "com.sun.jndi.ldap.LdapCtxFactory");
        env.put(Context.PROVIDER_URL, getProperty("ldap.url"));
        env.put(Context.SECURITY_AUTHENTICATION, "simple");
        env.put(Context.SECURITY_PRINCIPAL, getProperty("ldap.username"));
        env.put(Context.SECURITY_CREDENTIALS, getProperty("ldap.password"));
    }

    @Override
    public XacmlResponse askForPermission(XacmlRequest request) {
        final String saksbehandler = request.getRequest().getAccessSubject().getAttribute().stream()
                .filter(attribute -> attribute.getAttributeId().equals(StandardAttributter.SUBJECT_ID))
                //TODO or get from token
                .findFirst()
                .orElseThrow(() -> new RuntimeException("This should never happen. Subject id is missing in request."))
                .getValue();

        final boolean hasAccess = hasWantedGroupInAD(saksbehandler, WANTED_ATTRIBUTE);
        Decision decision = hasAccess ? Decision.Permit : Decision.Deny;
        List<Response> responses = new ArrayList<>();
        responses.add(new Response().withDecision(decision));
        return new XacmlResponse().withResponse(responses);
    }

    private boolean hasWantedGroupInAD(String veilederUid, String attributter) {
        try {
            String searchbase = "OU=Users,OU=NAV,OU=BusinessUnits," + getProperty("ldap.basedn");
            SearchControls searchCtrl = new SearchControls();
            searchCtrl.setSearchScope(SearchControls.SUBTREE_SCOPE);

            NamingEnumeration<SearchResult> result = ldapContext().search(searchbase, String.format("(&(objectClass=user)(CN=%s))", veilederUid), searchCtrl);
            Attributes ldapAttributes = result.next().getAttributes();
            return isMemberOf(attributter, ldapAttributes);

        } catch (NamingException e) {
            throw new RuntimeException(e);
        }
    }

    private boolean isMemberOf(String wantedAttribute, Attributes ldapAttributes) {
        final Attribute attribute = ldapAttributes.get(wantedAttribute);
        try {
            final NamingEnumeration<?> groups = attribute.getAll();
            while (groups.hasMore()) {
                final String group = groups.next().toString();
//                if (group.contains("AD-gruppe 0000-GA-Modia-Oppfolg-Pilot")) {
                if (group.contains("0000-GA-GOSYS_SENSITIVT")) {
                    return true;
                }
            }
        } catch (NamingException e) {
            throw new RuntimeException(e);
        }
        return false;
    }

    private static LdapContext ldapContext() throws NamingException {
        return new InitialLdapContext(env, null);
    }
}
