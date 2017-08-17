package no.nav.apiapp.util;

import no.nav.brukerdialog.security.context.SubjectHandler;
import no.nav.brukerdialog.security.domain.IdentType;

import java.util.Optional;

import static java.util.Optional.ofNullable;

public class SubjectUtils {


    public static Optional<IdentType> getIdentType() {
        return ofNullable(identType());
    }

    public static Optional<String> getUserId() {
        return ofNullable(userId());
    }

    private static String userId() {
        return ofNullable(SubjectHandler.getSubjectHandler().getUid())
                .orElseGet(() -> no.nav.modig.core.context.SubjectHandler.getSubjectHandler().getUid());
    }

    private static IdentType identType() {
        return ofNullable(SubjectHandler.getSubjectHandler().getIdentType())
                .orElseGet(() -> somBrukerDialogIdentType(no.nav.modig.core.context.SubjectHandler.getSubjectHandler().getIdentType()));
    }

    private static IdentType somBrukerDialogIdentType(no.nav.modig.core.domain.IdentType identType) {
        if (identType != null) {
            switch (identType) {
                case EksternBruker:
                    return IdentType.EksternBruker;
                case InternBruker:
                    return IdentType.InternBruker;
                case Samhandler:
                    return IdentType.Samhandler;
                case Sikkerhet:
                    return IdentType.Sikkerhet;
                case Systemressurs:
                    return IdentType.Systemressurs;
                case Prosess:
                    return IdentType.Prosess;
                default:
                    throw new IllegalStateException(identType.toString());
            }
        } else {
            return null;
        }
    }
}
