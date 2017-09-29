package no.nav.sbl.util;

public class ExceptionUtils {
    public static RuntimeException throwUnchecked(Throwable e) {
        return ExceptionUtils.genericThrow(e);
    }

    private static <T extends Throwable> T genericThrow(Throwable e) throws T {
        throw (T) e;
    }
}
