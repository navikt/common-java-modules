package no.nav.brukerdialog.security.context;

import no.nav.brukerdialog.security.domain.IdentType;
import no.nav.common.auth.SsoToken;
import no.nav.common.auth.Subject;

@Deprecated // bruk no.nav.common.auth.SubjectHandler
public class SubjectHandler {

    @Deprecated // bruk no.nav.common.auth.SubjectHandler
    public static SubjectHandler getSubjectHandler() {
        return new SubjectHandler();
    }

    @Deprecated // bruk no.nav.common.auth.SubjectHandler
    public static Subject getSubject() {
        return no.nav.common.auth.SubjectHandler.getSubject().orElse(null);
    }

    @Deprecated // bruk no.nav.common.auth.SubjectHandler
    public String getUid() {
        if (!hasSubject()) {
            return null;
        } else {
            return getSubject().getUid();
        }
    }

    @Deprecated // bruk no.nav.common.auth.SubjectHandler
    public IdentType getIdentType() {
        if (!hasSubject()) {
            return null;
        } else {
            return getSubject().getIdentType();
        }
    }

    @Deprecated // bruk no.nav.common.auth.SubjectHandler
    public String getInternSsoToken() {
        if (!hasSubject()) {
            return null;
        } else {
            return getSubject().getSsoToken(SsoToken.Type.OIDC).orElse(null);
        }
    }

    private Boolean hasSubject() {
        return getSubject() != null;
    }

}
