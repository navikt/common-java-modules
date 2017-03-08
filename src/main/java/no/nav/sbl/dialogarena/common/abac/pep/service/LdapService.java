package no.nav.sbl.dialogarena.common.abac.pep.service;

import no.nav.brukerdialog.security.context.SubjectHandler;
import no.nav.sbl.dialogarena.common.abac.pep.domain.request.XacmlRequest;
import no.nav.sbl.dialogarena.common.abac.pep.domain.response.*;

import javax.naming.NamingEnumeration;
import javax.naming.NamingException;
import javax.naming.directory.Attribute;
import javax.naming.directory.Attributes;
import java.util.ArrayList;
import java.util.List;

import static java.lang.System.getProperty;

public class LdapService implements TilgangService {

    static final String WANTED_ATTRIBUTE = "memberof";
    private final Ldap ldap;

    public LdapService(Ldap ldap) {

        this.ldap = ldap;
    }

    @Override
    public XacmlResponse askForPermission(XacmlRequest request) {
        String ident = SubjectHandler.getSubjectHandler().getUid();

        final Attributes attributes = ldap.getAttributes(ident);
        boolean hasAccess = isMemberOf(WANTED_ATTRIBUTE, attributes);

        Decision decision = hasAccess ? Decision.Permit : Decision.Deny;
        List<Response> responses = new ArrayList<>();
        responses.add(new Response().withDecision(decision));
        return new XacmlResponse().withResponse(responses).withFallbackUsed();
    }

    private boolean isMemberOf(String wantedAttribute, Attributes ldapAttributes) {
        final Attribute attribute = ldapAttributes.get(wantedAttribute);
        try {
            final NamingEnumeration<?> groups = attribute.getAll();
            while (groups.hasMore()) {
                final String group = groups.next().toString();
                if (group.contains(getProperty("role"))) {
                    return true;
                }
            }
        } catch (NamingException e) {
            throw new RuntimeException(e);
        }
        return false;
    }


}
