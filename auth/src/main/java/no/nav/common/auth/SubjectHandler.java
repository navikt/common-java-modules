package no.nav.common.auth;

import no.nav.brukerdialog.security.domain.IdentType;
import no.nav.sbl.util.fn.UnsafeRunnable;
import no.nav.sbl.util.fn.UnsafeSupplier;

import java.util.Optional;
import java.util.function.Supplier;

public class SubjectHandler {

    private static final SubjectStorage subjectStorage = new ThreadLocalSubjectStorage();

    public static void withSubject(Subject subject, UnsafeRunnable unsafeRunnable) {
        withSubject(subject, UnsafeSupplier.toVoid(unsafeRunnable));
    }

    public static <T> T withSubject(Subject subject, UnsafeSupplier<T> supplier) {
        return withSubjectProvider(() -> subject, supplier);
    }

    public static void withSubjectProvider(Supplier<Subject> subjectSupplier, UnsafeRunnable unsafeRunnable) {
        withSubjectProvider(subjectSupplier, UnsafeSupplier.toVoid(unsafeRunnable));
    }

    public static <T> T withSubjectProvider(Supplier<Subject> subjectSupplier, UnsafeSupplier<T> supplier) {
        Supplier<Subject> previousSubject = getSupplier();
        try {
            setSupplier(subjectSupplier);
            return supplier.get();
        } finally {
            setSupplier(previousSubject);
        }
    }

    protected static void setSupplier(Supplier<Subject> subjectSupplier) {
        subjectStorage.set(subjectSupplier);
    }

    protected static Supplier<Subject> getSupplier() {
        return subjectStorage.get();
    }

    public static Optional<Subject> getSubject() {
        return Optional.ofNullable(subjectStorage.get()).map(Supplier::get);
    }

    public static Optional<String> getIdent() {
        return getSubject().map(Subject::getUid);
    }

    public static Optional<IdentType> getIdentType() {
        return getSubject().map(Subject::getIdentType);
    }

    public static Optional<String> getSsoToken(SsoToken.Type type) {
        return getSubject().flatMap(s -> s.getSsoToken(type));
    }

    public static Optional<SsoToken> getSsoToken() {
        return getSubject().map(Subject::getSsoToken);
    }

    public interface SubjectStorage {
        Supplier<Subject> get();
        void set(Supplier<Subject> subject);
    }

    private static class ThreadLocalSubjectStorage implements SubjectStorage {
        private static final ThreadLocal<Supplier<Subject>> subjectHolder = new ThreadLocal<>();

        @Override
        public Supplier<Subject> get() {
            return subjectHolder.get();
        }

        @Override
        public void set(Supplier<Subject> subject) {
            subjectHolder.set(subject);
        }
    }


}
