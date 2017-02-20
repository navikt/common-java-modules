package no.nav.sbl.dialogarena.common.abac.pep.service;


import javax.inject.Inject;

import static java.util.Arrays.asList;

public class VeilederService {

    @Inject
    private LdapService ldapService;

    public boolean hasAccess(String veilederUid) {
        Object map = ldapService.hentVeilederAttributter(veilederUid, asList("role")).get("role");
        return true;
    }
}