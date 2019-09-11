package no.nav.util.sbl;

public class ExceptionUtils {
    public static RuntimeException throwUnchecked(Throwable e) {
        return ExceptionUtils.genericThrow(e);
    }

    private static <T extends Throwable> T genericThrow(Throwable e) throws T {
        throw (T) e;
    }

    public static Throwable getRootCause(Throwable throwable) {
        Throwable cause = throwable.getCause();
        if (cause != null) {
            return getRootCause(cause);
        } else {
            return throwable;
        }
    }

}
