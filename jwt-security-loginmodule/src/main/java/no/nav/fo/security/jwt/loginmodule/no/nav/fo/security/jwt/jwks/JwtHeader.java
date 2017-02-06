package no.nav.fo.security.jwt.loginmodule.no.nav.fo.security.jwt.jwks;

public class JwtHeader {
    private final String kid;
    private final String keyType;
    private final String use;
    private final String algorithm;

    public JwtHeader(String kid, String keyType, String use, String algorithm) {
        this.kid = kid;
        this.keyType = keyType;
        this.use = use;
        this.algorithm = algorithm;
    }

    public String getKid() {
        return kid;
    }

    public String getKeyType() {
        return keyType;
    }

    public String getUse() {
        return use;
    }

    public String getAlgorithm() {
        return algorithm;
    }

    @Override
    public String toString() {
        return JwtHeader.class.getSimpleName() + "{" +
                "kid='" + kid +
                ", keyType='" + keyType +
                ", use='" + use +
                ", algorithm='" + algorithm +
                '}';
    }
}
