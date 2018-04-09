package no.nav.brukerdialog.security.jwks;

import java.security.Key;

public enum DefaultJwksKeyHandler implements JwksKeyHandler {

    INSTANCE;

    private final JwksKeyHandlerImpl impl = new JwksKeyHandlerImpl();

    @Override
    public synchronized Key getKey(JwtHeader header) {
        return impl.getKey(header);
    }

}
