package no.nav.metrics.aspects;

import mockit.*;
import no.nav.metrics.*;
import org.aspectj.lang.ProceedingJoinPoint;
import org.junit.Test;

import static no.nav.metrics.TestUtil.lagAspectProxy;
import static org.junit.Assert.assertEquals;

public class CountAspectTest {


    @Test
    public void metoderMedCountAnnotasjonBlirTruffetAvAspect(@Mocked final CountAspect aspect) throws Throwable {
        new Expectations() {{
            aspect.countPaMetode((ProceedingJoinPoint) any, (Count) any);
            result = "proxyCount";
        }};

        CountMetoder countMetoder = lagAspectProxy(new CountMetoder(), aspect);

        assertEquals("proxyCount", countMetoder.count());
        assertEquals("ikkeCount", countMetoder.ikkeCount());
    }

    @Test
    public void metoderPaKlasseMedAnnotasjonBlirTruffetAvAspect(@Mocked final CountAspect aspect) throws Throwable {
        new Expectations() {{
            aspect.countPaKlasse((ProceedingJoinPoint) any, (Count) any);
            result = "proxyCount";
        }};

        CountKlasse proxy = lagAspectProxy(new CountKlasse(), aspect);

        assertEquals("proxyCount", proxy.count());
    }

    @Test
    public void metoderPaKlasseMedAnnotasjonBlirRiktigIgnorert(@Mocked final MetodeEvent event) throws Throwable {
        new Expectations() {{
            MetodeEvent.eventForMetode((Metodekall) any, anyString);
            result = "eventMetode";
        }};

        CountKlasseMedIgnorerteMetoder proxy = lagAspectProxy(new CountKlasseMedIgnorerteMetoder(), new CountAspect());

        assertEquals("eventMetode", proxy.event1());
        assertEquals("ignorert1", proxy.ignorert1());
    }

    @Test
    public void fieldsSattPaCountBlirInkludertIEventet(@Mocked final Event event) throws Throwable {
        CountMetoder countMetoder = lagAspectProxy(new CountMetoder(), new CountAspect());

        countMetoder.countMedFields("testArg1", "testArg2", "testArg3");

        new Verifications() {{
            event.addFieldToReport("customKey", "testArg2");

            event.addFieldToReport("str2", "testArg2");
            times = 0;
            event.addFieldToReport("str3", "testArg3");
            times = 0;
        }};
    }

    @Test
    public void countAspectLagerEventsMedRiktigeNavn(@Mocked final MetricsFactory factory) {
        CountAspect aspect = new CountAspect();

        CountMetoder countMetoder = lagAspectProxy(new CountMetoder(), aspect);
        countMetoder.count();
        countMetoder.countMedFields("", "", "");

        CountKlasse countKlasse = lagAspectProxy(new CountKlasse(), aspect);
        countKlasse.count();

        CountKlasseMedIgnorerteMetoder ignorerteMetoder = lagAspectProxy(new CountKlasseMedIgnorerteMetoder(), aspect);
        ignorerteMetoder.event1();

        new Verifications() {{
            MetricsFactory.createEvent("CountMetoder.count");
            MetricsFactory.createEvent("customName");

            MetricsFactory.createEvent("CountKlasse.count");

            MetricsFactory.createEvent("customName.event1");
        }};
    }

    private static class CountMetoder {
        @Count
        public String count() {
            return "count";
        }

        public String ikkeCount() {
            return "ikkeCount";
        }

        @Count(name = "customName", fields = @Field(key = "customKey", argumentNumber = "2"))
        public String countMedFields(String str1, String str2, String str3) {
            return "countMedFields";
        }
    }

    @Count
    private static class CountKlasse {
        public String count() {
            return "count";
        }
    }

    @Count(ignoredMethods = "ignorert1", name = "customName")
    private static class CountKlasseMedIgnorerteMetoder {
        public String event1() {
            return "event1";
        }

        public String ignorert1() {
            return "ignorert1";
        }
    }
}
