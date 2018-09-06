package no.nav.log;

import org.slf4j.MDC;

import java.util.Map;

public class MDCUtils {


    public static void withContext(Map<String, String> context, Runnable runnable) {
        Map<String, String> originalContext = MDC.getCopyOfContextMap();
        setContext(context);
        try {
            runnable.run();
        } finally {
            setContext(originalContext);
        }
    }

    private static void setContext(Map<String, String> context) {
        if (context == null) {
            MDC.clear();
        } else {
            MDC.setContextMap(context);
        }
    }


}
