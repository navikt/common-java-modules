package no.nav.common.log;

import static no.nav.common.log.LogUtils.runWithMDCContext;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;

public class LogUtilsTest {

    @Test
    public void runWithMDCContext_skal_legge_til_MDC_variabler() throws InterruptedException {
        MDC.clear();
        MDC.put("TEST", "hello");

        Map<String, String> contextMap = MDC.getCopyOfContextMap();

        AtomicReference<String> mdcValueRef = new AtomicReference<>();
        CountDownLatch countDownLatch = new CountDownLatch(1);

        new Thread(() -> {
                    runWithMDCContext(contextMap, () -> {
                        mdcValueRef.set(MDC.get("TEST"));
                        countDownLatch.countDown();
                    });
                })
                .start();

        countDownLatch.await();

        assertEquals("hello", mdcValueRef.get());
    }

    @Test
    public void runWithMDCContext_skal_rydde_opp_context() throws InterruptedException {
        MDC.clear();

        Map<String, String> contextMap = MDC.getCopyOfContextMap();

        AtomicReference<String> mdcValueRef = new AtomicReference<>();
        CountDownLatch countDownLatch = new CountDownLatch(1);

        new Thread(() -> {
                    runWithMDCContext(contextMap, () -> {
                        MDC.put("TEST1", "hello1");
                    });
                    mdcValueRef.set(MDC.get("TEST1"));
                    countDownLatch.countDown();
                })
                .start();

        countDownLatch.await();

        assertNull(mdcValueRef.get());
    }
}
