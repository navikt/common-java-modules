package no.nav.brukerdialog.security.pingable;

import no.nav.brukerdialog.security.oidc.SystemUserTokenProvider;
import no.nav.brukerdialog.security.oidc.SystemUserTokenProviderConfig;
import no.nav.fasit.FasitUtils;
import no.nav.fasit.ServiceUser;
import no.nav.fasit.dto.RestService;
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
import static no.nav.sbl.dialogarena.test.FasitAssumption.assumeFasitAccessible;
import static no.nav.sbl.util.LogUtils.setGlobalLogLevel;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assume.assumeFalse;


public class IssoSystemBrukerTokenHelsesjekkTest {

    private static final Logger LOGGER = LoggerFactory.getLogger(IssoSystemBrukerTokenHelsesjekkTest.class);

    private static SystemUserTokenProviderConfig systemUserTokenProviderConfig;

    private IssoSystemBrukerTokenHelsesjekk issoSystemBrukerTokenHelsesjekk = new IssoSystemBrukerTokenHelsesjekk(new SystemUserTokenProvider(systemUserTokenProviderConfig));
    private Throwable error;

    @BeforeClass
    public static void setup() {
        assumeFasitAccessible();
        assumeFalse(FasitUtils.usingMock());
        systemUserTokenProviderConfig = resolveValidConfiguration();
        setGlobalLogLevel(INFO);
        setProperty(REFRESH_TIME, Integer.toString(MAX_VALUE / 2000)); // slik at hver ping fører til refresh, se SystemUserTokenProvider.tokenIsSoonExpired()
    }

    private static SystemUserTokenProviderConfig resolveValidConfiguration() {
        String applicationName = "veilarbdemo";
        ServiceUser serviceUser = FasitUtils.getServiceUser("srvveilarbdemo", applicationName);
        String issoHost = FasitUtils.getBaseUrl("isso-host");
        String issoJWS = FasitUtils.getBaseUrl("isso-jwks");
        String issoISSUER = FasitUtils.getBaseUrl("isso-issuer");
        ServiceUser isso_rp_user = FasitUtils.getServiceUser("isso-rp-user", applicationName);
        RestService loginUrl = FasitUtils.getRestService("veilarblogin.redirect-url", FasitUtils.getDefaultEnvironment());
        return SystemUserTokenProviderConfig.builder()
                .issoJwksUrl(issoJWS)
                .issoHostUrl(issoHost)
                .issoExpectedTokenIssuer(issoISSUER)
                .oidcRedirectUrl(loginUrl.getUrl())
                .srvUsername(serviceUser.getUsername())
                .srvPassword(serviceUser.getPassword())
                .issoRpUserUsername(isso_rp_user.getUsername())
                .issoRpUserPassword(isso_rp_user.getPassword())
                .build();
    }

    @Test
    public void smoketest() throws InterruptedException {
        ExecutorService executorService = newFixedThreadPool(100);
        for (int i = 0; i < 200; i++) {
            int nr = i;
            executorService.submit(() -> {
                ping();
                LOGGER.info("{}", nr);
                System.gc();
            });
        }
        executorService.shutdown();
        executorService.awaitTermination(1, MINUTES);
        assertThat(error).isNull();
    }

    private void ping() {
        try {
            Pingable.Ping ping = issoSystemBrukerTokenHelsesjekk.ping();
            ofNullable(ping.getFeil()).ifPresent(ExceptionUtils::throwUnchecked);
            assertThat(ping.erVellykket()).isTrue();
        } catch (Throwable t) {
            LOGGER.error(t.getMessage(), t);
            error = t;
        }
    }

}