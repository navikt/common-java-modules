package no.nav.common.log;

import no.nav.common.utils.fn.UnsafeRunnable;
import no.nav.common.utils.fn.UnsafeSupplier;
import org.slf4j.MDC;
import org.slf4j.spi.MDCAdapter;

import java.util.Map;

public class LogUtils {

    public static void runWithMDCContext(Map<String, String> mdcContext, UnsafeRunnable runnable) {
        runWithMDCContext(mdcContext, () -> {
            runnable.run();
            return null;
        });
    }

    // Brukes for å propagere MDC context videre til f.eks tråder som kjøres fra et thread pool
    public static <T> T runWithMDCContext(Map<String, String> mdcContext, UnsafeSupplier<T> supplier) {
        MDCAdapter mdcAdapter = MDC.getMDCAdapter();
        Map<String, String> prevContext = MDC.getCopyOfContextMap();

        try {
            if (mdcAdapter != null && mdcContext != null) {
                mdcAdapter.setContextMap(mdcContext);
            }

            return supplier.get();
        } finally {
            if (mdcAdapter != null) {
                if (prevContext != null) {
                    mdcAdapter.setContextMap(prevContext);
                } else {
                    mdcAdapter.clear();
                }
            }
        }
    }

}
