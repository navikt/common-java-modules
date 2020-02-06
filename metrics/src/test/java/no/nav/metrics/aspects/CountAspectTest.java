package no.nav.metrics.aspects;

import no.nav.metrics.Metodekall;
import org.aspectj.lang.ProceedingJoinPoint;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Map;

import static no.nav.metrics.TestUtil.lagAspectProxy;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.hasEntry;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;

public class CountAspectTest {

    @Test
    public void metoderMedCountAnnotasjonBlirTruffetAvAspect() throws Throwable {
        CountAspect aspect = new CountAspect() {
            @Override
            public Object countPaMetode(ProceedingJoinPoint joinPoint, Count count) throws Throwable {
                return "proxyCount";
            }
        };

        CountMetoder countMetoder = lagAspectProxy(new CountMetoder(), aspect);

        assertEquals("proxyCount", countMetoder.count());
        assertEquals("ikkeCount", countMetoder.ikkeCount());
    }

    @Test
    public void metoderPaKlasseMedAnnotasjonBlirTruffetAvAspect() throws Throwable {
        CountAspect aspect = new CountAspect() {
            @Override
            public Object countPaKlasse(ProceedingJoinPoint joinPoint, Count count) throws Throwable {
                return "proxyCount";
            }
        };
        CountKlasse proxy = lagAspectProxy(new CountKlasse(), aspect);

        assertEquals("proxyCount", proxy.count());
    }

    @Test
    public void metoderPaKlasseMedAnnotasjonBlirRiktigIgnorert() throws Throwable {
        CountAspect aspect = new CountAspect() {
            @Override
            Object eventForMetode(Metodekall metodekall, String eventNavn) throws Throwable {
                return "eventMetode";
            }
        };

        CountKlasseMedIgnorerteMetoder proxy = lagAspectProxy(new CountKlasseMedIgnorerteMetoder(), aspect);

        assertEquals("eventMetode", proxy.event1());
        assertEquals("ignorert1", proxy.ignorert1());
    }

    @Test
    public void fieldsSattPaCountBlirInkludertIEventet() throws Throwable {
        CountAspect aspect = new CountAspect() {
            @Override
            Object eventForMetode(Metodekall metodekall, String eventNavn, Map<String, String> verdier) throws Throwable {
                assertThat(verdier, hasEntry("customKey", "testArg2"));
                assertEquals(verdier.size(), 1);
                return super.eventForMetode(metodekall, eventNavn, verdier);
            }
        };

        CountMetoder countMetoder = lagAspectProxy(new CountMetoder(), aspect);

        countMetoder.countMedFields("testArg1", "testArg2", "testArg3");
    }

    @Test
    public void countAspectLagerEventsMedRiktigeNavn() {
        final ArrayList<String> methodNames = new ArrayList<>();

        CountAspect aspect = new CountAspect() {
            @Override
            Object eventForMetode(Metodekall metodekall, String eventNavn, Map<String, String> verdier) throws Throwable {
                methodNames.add(eventNavn);
                return super.eventForMetode(metodekall, eventNavn, verdier);
            }
        };

        CountMetoder countMetoder = lagAspectProxy(new CountMetoder(), aspect);
        countMetoder.count();
        countMetoder.countMedFields("", "", "");

        CountKlasse countKlasse = lagAspectProxy(new CountKlasse(), aspect);
        countKlasse.count();

        CountKlasseMedIgnorerteMetoder ignorerteMetoder = lagAspectProxy(new CountKlasseMedIgnorerteMetoder(), aspect);
        ignorerteMetoder.event1();

        assertThat(methodNames, containsInAnyOrder("CountMetoder.count", "customName", "CountKlasse.count", "customName.event1"));
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
