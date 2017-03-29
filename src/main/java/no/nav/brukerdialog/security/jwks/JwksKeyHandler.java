package no.nav.brukerdialog.security.jwks;

import java.security.Key;

public interface JwksKeyHandler {
    Key getKey(JwtHeader header);
}
