package no.nav.sbl.dialogarena.time;

import org.joda.time.DateMidnight;
import org.joda.time.DateTime;
import org.junit.Before;
import org.junit.Test;

import static org.hamcrest.Matchers.is;
import static org.joda.time.Duration.standardDays;
import static org.joda.time.Duration.standardHours;
import static org.junit.Assert.assertThat;


public class FreezedClockTest {

    private final DateTime time = new DateTime(2013, 3, 18, 14, 30);

    private FreezedClock clock = new FreezedClock();

    @Before
    public void initClock() {
        clock = new FreezedClock();
    }

    @Test(expected = IllegalStateException.class)
    public void mustBeInitialized() {
        clock.now();
    }

    @Test
    public void nowIsFreezed() {
        clock.set(time);
        assertThat(clock.now(), is(time));
        assertThat(clock.now(), is(time));
        assertThat(clock.now(), is(time));
        assertThat(clock.now(), is(time));
    }

    @Test
    public void behavesLikeThis() {
        clock.set(time);
        clock.tick(standardDays(1).plus(standardHours(2)));

        assertThat(clock.now().getDayOfMonth(), is(time.getDayOfMonth() + 1));
        assertThat(clock.now().getHourOfDay(), is(time.getHourOfDay() + 2));
    }

    @Test
    public void canSetMidnight() {
        clock.set(new DateMidnight(time));
        assertThat(clock.now().getDayOfMonth(), is(time.getDayOfMonth()));
        assertThat(clock.now().getHourOfDay(), is(0));
        assertThat(clock.now().getMinuteOfDay(), is(0));
    }
}
