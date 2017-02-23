package no.nav.security.jwt.security.domain;

public class IdTokenAndRefreshToken {
    private JwtCredential idToken;
    private String refreshToken;

    public IdTokenAndRefreshToken(JwtCredential idToken, String refreshToken) {
        this.idToken = idToken;
        this.refreshToken = refreshToken;
    }

    public JwtCredential getIdToken() {
        return idToken;
    }

    public String getRefreshToken() {
        return refreshToken;
    }
}
