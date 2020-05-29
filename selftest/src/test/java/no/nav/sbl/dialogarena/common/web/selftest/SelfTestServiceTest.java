package no.nav.sbl.dialogarena.common.web.selftest;

import no.nav.sbl.dialogarena.common.web.selftest.domain.Selftest;
import no.nav.sbl.dialogarena.common.web.selftest.domain.SelftestResult;
import no.nav.sbl.dialogarena.types.Pingable;
import org.junit.Test;

import java.util.List;

import static java.util.Arrays.asList;
import static no.nav.sbl.dialogarena.common.web.selftest.SelfTestStatus.ERROR;
import static no.nav.sbl.dialogarena.common.web.selftest.SelfTestStatus.OK;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class SelfTestServiceTest {


    private final Pingable.Ping.PingMetadata pingMetadata = new Pingable.Ping.PingMetadata("test", "test", "test", true);

    @Test
    public void selfTest__ok() {
        Pingable pingable = mock(Pingable.class);
        when(pingable.ping()).thenReturn(Pingable.Ping.lyktes(pingMetadata));

        SelfTestService selfTestService = service(pingable);

        Selftest selftest = selfTestService.selfTest();
        assertThat(selftest.getAggregateResult()).isEqualTo(OK);
        check(selftest);
    }

    @Test
    public void selfTest__handle_null_values() {
        SelfTestService selfTestService = service(mock(Pingable.class));
        Selftest selftest = selfTestService.selfTest();
        assertThat(selftest.getAggregateResult()).isEqualTo(ERROR);
        check(selftest);
    }

    @Test
    public void selfTest__handle_exceptions() {
        Pingable pingableA = mock(Pingable.class);
        when(pingableA.ping()).thenThrow(RuntimeException.class);

        SelfTestService selfTestService = service(pingableA);

        Selftest selftest = selfTestService.selfTest();
        assertThat(selftest.getAggregateResult()).isEqualTo(ERROR);
        check(selftest);
    }

    private void check(Selftest selftest) {
        List<SelftestResult> checks = selftest.getChecks();
        assertThat(checks).isNotEmpty();
        checks.forEach(selftestResult -> {
            assertThat(selftestResult.getId()).isNotEmpty();
            assertThat(selftestResult.getResult()).isNotNull();
        });
    }

    private SelfTestService service(Pingable... pingables) {
        return new SelfTestService(asList(pingables));
    }

}