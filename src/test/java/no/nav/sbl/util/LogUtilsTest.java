package no.nav.sbl.util;

import org.junit.Test;

import static ch.qos.logback.classic.Level.ERROR;
import static ch.qos.logback.classic.Level.INFO;
import static no.nav.sbl.util.LogUtils.setGlobalLogLevel;

public class LogUtilsTest {

    @Test
    public void smoketest() {
        setGlobalLogLevel(INFO);
        setGlobalLogLevel(ERROR);
    }

}