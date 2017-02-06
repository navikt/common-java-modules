package no.nav.sbl.dialogarena.common.abac.pep;

public class Client {

    private String oidcToken, subjectId, domain, fnr, credentialResource;

    public String getOidcToken() { return oidcToken; }

    public String getSubjectId() { return subjectId; }

    public String getDomain() { return domain; }

    public String getFnr() { return fnr; }

    public String getCredentialResource() { return credentialResource; }

    public Client withOidcToken(String oidcToken) {
        this.oidcToken = oidcToken;
        return this;
    }

    public Client withSubjectId(String subjectId) {
        this.subjectId = subjectId;
        return this;
    }

    public Client withCredentialResource(String credentialResource) {
        this.credentialResource = credentialResource;
        return this;
    }

    public Client withDomain(String domain) {
        this.domain = domain;
        return this;
    }

    public Client withFnr(String fnr) {
        this.fnr = fnr;
        return this;
    }
}
