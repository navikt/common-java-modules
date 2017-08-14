package no.nav.sbl.dialogarena.common.web.selftest;

import lombok.SneakyThrows;
import no.nav.sbl.dialogarena.types.Pingable;
import no.nav.sbl.dialogarena.types.Pingable.Ping.PingMetadata;
import org.junit.Before;
import org.junit.Test;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.CharArrayWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Collection;
import java.util.HashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicInteger;

import static java.util.Arrays.asList;
import static java.util.concurrent.Executors.newFixedThreadPool;
import static java.util.stream.Collectors.toList;
import static java.util.stream.IntStream.range;
import static no.nav.sbl.dialogarena.common.web.selftest.SelfTestBaseServlet.STATUS_ERROR;
import static no.nav.sbl.dialogarena.common.web.selftest.SelfTestServletTest.TestPingable.PING_TID;
import static no.nav.sbl.dialogarena.types.Pingable.Ping;
import static org.hamcrest.Matchers.lessThan;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class SelfTestServletTest {

    private TestPingable pingA = new TestPingable(Ping.lyktes(new PingMetadata("a", "beskrivelse", true)));
    private TestPingable pingB = new TestPingable(Ping.lyktes(new PingMetadata("b", "beskrivelse", true)));
    private TestPingable pingC = new TestPingable(Ping.feilet(new PingMetadata("c", "beskrivelse", true), new IllegalArgumentException("Cfeil")));

    private HttpServletRequest mockRequest;
    private HttpServletResponse mockResponse;
    private SelfTestBaseServlet baseServlet;

    @Before
    public void setUp() throws Exception {
        baseServlet = createBaseServlet();

        mockRequest = mock(HttpServletRequest.class);
        mockResponse = mock(HttpServletResponse.class);

        when(mockRequest.getParameterMap()).thenReturn(new HashMap<>());
        when(mockResponse.getWriter()).thenReturn(createMockedPrintWriter());
    }

    @Test
    public void testSelfTestBase() throws ServletException, IOException {
        baseServlet.doGet(mockRequest, mockResponse);

        assertNotNull(baseServlet);
        assertThat(baseServlet.getApplicationName(), is("TestApp"));
        assertThat(baseServlet.getApplicationVersion(), is("unknown version"));
        assertTrue(baseServlet.getHost().endsWith(".devillo.no"));
        assertThat(baseServlet.getAggregertStatus(), is(STATUS_ERROR));
        assertThat(baseServlet.getPingables().size(), is(3));
    }

    @Test(timeout = PING_TID * 10)
    public void beskyttPingablesMotMangeSamtidigeRequesterMenBevarHoyThroughput() throws ServletException, IOException {
        ExecutorService executorService = newFixedThreadPool(100);
        range(0, 100)
                .mapToObj((i) -> executorService.submit(this::get))
                .collect(toList()).stream() // tvinger alle submits før vi resolver
                .forEach(SelfTestServletTest::resolveFuture);

        assertThat(pingA.counter.get(), lessThan(5));
        assertThat(pingB.counter.get(), lessThan(5));
        assertThat(pingC.counter.get(), lessThan(5));
        executorService.shutdown();
    }

    @SneakyThrows
    private void get() {
        System.out.println(Thread.currentThread());
        baseServlet.doGet(mockRequest, mockResponse);
    }

    private PrintWriter createMockedPrintWriter() {
        return new PrintWriter(new CharArrayWriter());
    }

    private SelfTestBaseServlet createBaseServlet() {
        return new SelfTestBaseServlet() {
            @Override
            protected String getApplicationName() {
                return "TestApp";
            }

            @Override
            protected Collection<TestPingable> getPingables() {
                return asList(
                        pingA,
                        pingB,
                        pingC
                );
            }
        };
    }

    static class TestPingable implements Pingable {

        static final long PING_TID = 100L;

        private Ping ping;
        private AtomicInteger counter = new AtomicInteger();

        private TestPingable(Ping ping) {
            this.ping = ping;
        }

        @Override
        @SneakyThrows
        public Ping ping() {
            Thread.sleep(PING_TID);
            counter.incrementAndGet();
            return ping;
        }
    }

    @SneakyThrows
    private static void resolveFuture(Future future) {
        future.get();
    }

}
