package no.nav.brukerdialog.security.pingable;

import no.nav.dialogarena.config.fasit.FasitUtils;
import no.nav.sbl.dialogarena.types.Pingable;
import no.nav.sbl.util.ExceptionUtils;
import org.junit.BeforeClass;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.ExecutorService;

import static ch.qos.logback.classic.Level.INFO;
import static java.lang.Integer.MAX_VALUE;
import static java.lang.System.setProperty;
import static java.util.Optional.ofNullable;
import static java.util.concurrent.Executors.newFixedThreadPool;
import static java.util.concurrent.TimeUnit.MINUTES;
import static no.nav.brukerdialog.security.Constants.REFRESH_TIME;
import static no.nav.dialogarena.config.fasit.FasitUtils.getApplicationEnvironment;
import static no.nav.sbl.util.LogUtils.setGlobalLogLevel;
import static org.assertj.core.api.Assertions.assertThat;


public class IssoSystemBrukerTokenHelsesjekkTest {

    private static final Logger LOGGER = LoggerFactory.getLogger(IssoSystemBrukerTokenHelsesjekkTest.class);

    private IssoSystemBrukerTokenHelsesjekk issoSystemBrukerTokenHelsesjekk = new IssoSystemBrukerTokenHelsesjekk();

    @BeforeClass
    public static void setup() {
        System.getProperties().putAll(getApplicationEnvironment("veilarbaktivitet"));
        setGlobalLogLevel(INFO);
        setProperty(REFRESH_TIME, Integer.toString(MAX_VALUE / 2000)); // slik at hver ping fører til refresh, se SystemUserTokenProvider.tokenIsSoonExpired()
    }

    @Test
    public void smoketest() throws InterruptedException {
        ExecutorService executorService = newFixedThreadPool(100);
        for (int i = 0; i < 200; i++) {
            int nr = i;
            executorService.submit(()->{
                ping();
                LOGGER.info("{}", nr);
                System.gc();
            });
        }
        executorService.shutdown();
        executorService.awaitTermination(1, MINUTES);
    }

    private void ping() {
        Pingable.Ping ping = issoSystemBrukerTokenHelsesjekk.ping();
        ofNullable(ping.getFeil()).ifPresent(ExceptionUtils::throwUnchecked);
        assertThat(ping.erVellykket()).isTrue();
    }

}