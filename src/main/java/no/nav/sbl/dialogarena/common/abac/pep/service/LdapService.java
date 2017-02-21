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
import static java.util.Optional.ofNullable;

//må bruke Hashtable i InitiallLdapContext dessverre.
@SuppressWarnings({"squid:S1149"})
public class LdapService implements TilgangService {

    private static Hashtable<String, String> env = new Hashtable<>();

    static {
        env.put(Context.INITIAL_CONTEXT_FACTORY, "com.sun.jndi.ldap.LdapCtxFactory");
        env.put(Context.PROVIDER_URL, getProperty("ldap.url"));
        env.put(Context.SECURITY_AUTHENTICATION, "simple");
        env.put(Context.SECURITY_PRINCIPAL, getProperty("ldap.username"));
        env.put(Context.SECURITY_CREDENTIALS, getProperty("ldap.password"));
    }

    @Override
    public XacmlResponse askForPermission(XacmlRequest request) {
        final String saksbehandler = request.getRequest().getEnvironment().getAttribute().stream()
                .filter(attribute -> attribute.getAttributeId().equals(StandardAttributter.SUBJECT_ID))
                .findFirst()
                .orElseThrow(() -> new RuntimeException("This should never happen. Subject id is missing in request."))
                .getValue();

        final boolean hasAccess = hasAccess(saksbehandler);
        Decision decision = hasAccess ? Decision.Permit : Decision.Deny;
        List<Response> responses = new ArrayList<>();
        responses.add(new Response().withDecision(decision));
        return new XacmlResponse().withResponse(responses);
    }

    private boolean hasAccess(String veilederUid) {
        List<String> attributes = new ArrayList<>();
        attributes.add("role");
        Object map = hentVeilederAttributter(veilederUid, attributes).get("role");

        //TODO sjekke om saksbehandler har riktig rolle
        return true;
    }

    private Map hentVeilederAttributter(String veilederUid, List<String> attributter) {
        Map map = new HashMap<>();

        try {
            String searchbase = "OU=Users,OU=NAV,OU=BusinessUnits," + getProperty("ldap.basedn");
            SearchControls searchCtrl = new SearchControls();
            searchCtrl.setSearchScope(SearchControls.SUBTREE_SCOPE);

            NamingEnumeration<SearchResult> result = ldapContext().search(searchbase, String.format("(&(objectClass=user)(CN=%s))", veilederUid), searchCtrl);
            Attributes ldapAttributes = result.next().getAttributes();
            populateAttributtMap(attributter, map, ldapAttributes);

        } catch (NamingException e) {
            throw new RuntimeException(e);
        }
        return map;
    }

    private void populateAttributtMap(List<String> attributter, Map map, Attributes ldapAttributes) {
        attributter.forEach(s -> {
            try {
                map.put(s, ofNullable(ldapAttributes.get(s).get())
                        .orElseThrow(() -> new RuntimeException("Fant ikke attributt " + s)));
            } catch (NamingException e) {
                throw new RuntimeException(e);
            }
        });
    }

    private static LdapContext ldapContext() throws NamingException {
        return new InitialLdapContext(env, null);
    }
}
