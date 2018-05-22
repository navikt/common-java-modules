package no.nav.common.auth;

import lombok.Value;
import lombok.experimental.Wither;
import no.nav.brukerdialog.security.domain.IdentType;
import no.nav.sbl.util.AssertUtils;
import no.nav.sbl.util.StringUtils;

import java.util.Optional;

import static java.util.Optional.empty;
import static no.nav.sbl.util.AssertUtils.assertNotNull;
import static no.nav.sbl.util.StringUtils.assertNotNullOrEmpty;

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
