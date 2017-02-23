package no.nav.security.jwt.security.jwks;

public class JwksKeyHandlers {

    public static JwksKeyHandler createKeyHandler() {
        return new JwksKeyHandler(System.getProperty("isso-jwks.url"));
    }

}
