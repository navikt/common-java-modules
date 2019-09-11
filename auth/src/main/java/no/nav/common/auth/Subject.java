package no.nav.common.auth;

import lombok.Value;
import lombok.experimental.Wither;
import no.nav.brukerdialog.security.domain.IdentType;
import no.nav.util.sbl.StringUtils;

import java.util.Optional;

import static java.util.Optional.empty;
import static no.nav.util.sbl.AssertUtils.assertNotNull;
import static no.nav.util.sbl.StringUtils.assertNotNullOrEmpty;

@Wither
@Value
public class Subject {

    private final String uid;
    private final IdentType identType;
    private final SsoToken ssoToken;

    public Subject(String uid, IdentType identType, SsoToken ssoToken) {
        assertNotNullOrEmpty(uid);
        assertNotNull(identType);
        assertNotNull(ssoToken);

        this.uid = uid;
        this.identType = identType;
        this.ssoToken = ssoToken;
    }

    public Optional<String> getSsoToken(SsoToken.Type type) {
        return ssoToken.getType() == type ? StringUtils.of(ssoToken.getToken()) : empty();
    }

}
