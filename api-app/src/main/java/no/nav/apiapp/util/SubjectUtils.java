package no.nav.apiapp.util;

import no.nav.brukerdialog.security.domain.IdentType;
import no.nav.common.auth.Subject;
import no.nav.common.auth.SubjectHandler;

import java.util.Optional;

@Deprecated // bruk SubjectHandler direkte
public class SubjectUtils {

    public static Optional<IdentType> getIdentType() {
        return SubjectHandler.getIdentType();
    }

    public static Optional<String> getUserId() {
        return SubjectHandler.getIdent();
    }

}
