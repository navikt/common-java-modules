package no.nav.sbl.dialogarena.time;

import org.joda.time.LocalDateTime;
import org.junit.Test;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;



public class SystemClockTest {

    private Clock clock = new SystemClock();

    @Test
    public void returnsTheTimeUsingSystemClock() {
        assertThat(clock.now().getDayOfMonth(), is(LocalDateTime.now().getDayOfMonth()));
        assertThat(clock.now().getMonthOfYear(), is(LocalDateTime.now().getMonthOfYear()));
    }


}

