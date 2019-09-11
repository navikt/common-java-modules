package no.nav.sbl.util;

import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;

import static ch.qos.logback.classic.Level.ERROR;
import static ch.qos.logback.classic.Level.INFO;
import static no.nav.log.sbl.LogUtils.buildMarker;
import static no.nav.log.sbl.LogUtils.setGlobalLogLevel;

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

}
