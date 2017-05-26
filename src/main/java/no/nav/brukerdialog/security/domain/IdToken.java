package no.nav.brukerdialog.security.domain;

public class IdToken {
    private OidcCredential idToken;
    private long expirationTime;

    public IdToken(OidcCredential idToken, long expirationTime) {
        this.idToken = idToken;
        this.expirationTime = expirationTime;
    }

    public OidcCredential getIdToken() {
        return idToken;
    }

    public long getExpirationTimeSeconds() {
        return expirationTime;
    }
}
