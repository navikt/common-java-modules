package no.nav.sbl.dialogarena.common.abac.pep;

import lombok.Getter;
import lombok.Setter;
import no.nav.sbl.dialogarena.common.abac.pep.domain.ResourceType;

@Getter
@Setter
public class Client {

    private String oidcToken;
    private String subjectId;
    private String domain;
    private String fnr;
    private ResourceType resourceType;
    private String credentialResource;

    Client(String oidcToken, String subjectId, String fnr, ResourceType resourceType, String domain, String credentialResource) {

        this.oidcToken = oidcToken;
        this.subjectId = subjectId;
        this.fnr = fnr;
        this.resourceType = resourceType;
        this.domain = domain;
        this.credentialResource = credentialResource;
    }

    Client() {
    }

    Client withOidcToken(String oidcToken) {
        this.oidcToken = oidcToken;
        return this;
    }

    Client withSubjectId(String subjectId) {
        this.subjectId = subjectId;
        return this;
    }

    Client withCredentialResource(String credentialResource) {
        this.credentialResource = credentialResource;
        return this;
    }

    Client withDomain(String domain) {
        this.domain = domain;
        return this;
    }

}
