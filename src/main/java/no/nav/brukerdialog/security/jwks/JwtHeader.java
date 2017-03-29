package no.nav.brukerdialog.security.jwks;

public class JwtHeader {
    private final String kid;
    private final String algorithm;

    public JwtHeader(String kid, String algorithm) {
        this.kid = kid;
        this.algorithm = algorithm;
    }

    public String getKid() {
        return kid;
    }

    public String getAlgorithm() {
        return algorithm;
    }

    @Override
    public String toString() {
        return JwtHeader.class.getSimpleName() + "{" +
                "kid='" + kid +
                ", algorithm='" + algorithm +
                '}';
    }
}
