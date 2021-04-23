package no.nav.common.log;

import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;

import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicReference;

import static ch.qos.logback.classic.Level.ERROR;
import static ch.qos.logback.classic.Level.INFO;
import static no.nav.common.log.LogUtils.*;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

public class LogUtilsTest {


    private static final Logger LOGGER = LoggerFactory.getLogger(LogUtilsTest.class);

    private static final String FNR = "12345678901";

    @Test
    public void smoketest() {
        setGlobalLogLevel(INFO);

        MDC.put(LogUtilsTest.class.getName(), FNR);
        LOGGER.info(FNR);
        LOGGER.info("fnr: {}", FNR);
        LOGGER.info("<tag>{}</tag>", FNR);
        LOGGER.info("info");

        LOGGER.error(FNR, new RuntimeException(FNR, new IllegalArgumentException(FNR)));

        buildMarker()
                .field("fnr2",FNR)
                .field("number",42)
                .field("a","b")
                .field("c",null)
                .log(LOGGER::info);

        buildMarker()
                .log(LOGGER::info)
                .field("more","stuff")
                .log(LOGGER::error);

        setGlobalLogLevel(ERROR);
        LOGGER.info("info");
    }

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
        }).start();

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
        }).start();

        countDownLatch.await();

        assertNull(mdcValueRef.get());
    }

}