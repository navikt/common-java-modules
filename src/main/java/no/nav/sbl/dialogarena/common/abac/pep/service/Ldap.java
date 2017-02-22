package no.nav.sbl.dialogarena.common.abac.pep.service;


import javax.naming.*;
import javax.naming.directory.*;
import javax.naming.ldap.InitialLdapContext;
import javax.naming.ldap.LdapContext;
import java.util.Hashtable;

import static java.lang.System.getProperty;

public class Ldap {
    private static final Hashtable<String, String> env = new Hashtable<>();

    static {
        env.put(Context.INITIAL_CONTEXT_FACTORY, "com.sun.jndi.ldap.LdapCtxFactory");
        env.put(Context.PROVIDER_URL, getProperty("ldap.url"));
        env.put(Context.SECURITY_AUTHENTICATION, "simple");
        env.put(Context.SECURITY_PRINCIPAL, getProperty("ldap.username"));
        env.put(Context.SECURITY_CREDENTIALS, getProperty("ldap.password"));
    }

    private static LdapContext ldapContext() throws NamingException {
        return new InitialLdapContext(env, null);
    }

    public Attributes getAttributes(String veilederUid) {
        try {
            String searchbase = "OU=Users,OU=NAV,OU=BusinessUnits," + getProperty("ldap.basedn");
            SearchControls searchCtrl = new SearchControls();
            searchCtrl.setSearchScope(SearchControls.SUBTREE_SCOPE);

            NamingEnumeration<SearchResult> result = ldapContext().search(searchbase, String.format("(&(objectClass=user)(CN=%s))", veilederUid), searchCtrl);
            return result.next().getAttributes();

        } catch (NamingException e) {
            throw new RuntimeException(e);
        }
    }
}
