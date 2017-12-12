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

import static java.lang.System.clearProperty;
import static java.lang.System.setProperty;
import static java.util.Arrays.asList;
import static java.util.concurrent.Executors.newFixedThreadPool;
import static java.util.stream.Collectors.toList;
import static java.util.stream.IntStream.range;
import static no.nav.sbl.dialogarena.common.web.selftest.SelfTestBaseServlet.APP_VERSION_PROPERTY_NAME;
import static no.nav.sbl.dialogarena.common.web.selftest.SelfTestBaseServlet.STATUS_ERROR;
import static no.nav.sbl.dialogarena.common.web.selftest.SelfTestServletTest.TestPingable.PING_TID;
import static no.nav.sbl.dialogarena.types.Pingable.Ping;
import static org.assertj.core.api.Assertions.assertThat;
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
        clearProperty(APP_VERSION_PROPERTY_NAME);
        baseServlet = createBaseServlet();

        mockRequest = mock(HttpServletRequest.class);
        mockResponse = mock(HttpServletResponse.class);

        when(mockRequest.getParameterMap()).thenReturn(new HashMap<>());
        when(mockResponse.getWriter()).thenReturn(createMockedPrintWriter());
    }

    @Test
    public void testSelfTestBase() throws ServletException, IOException {
        baseServlet.doGet(mockRequest, mockResponse);

        assertThat(baseServlet).isNotNull();
        assertThat(baseServlet.getApplicationName()).isEqualTo("TestApp");
        assertThat(baseServlet.getApplicationVersion()).isEqualTo("unknown version");
        assertThat(baseServlet.getHost()).isNotBlank();
        assertThat(baseServlet.getAggregertStatus()).isEqualTo(STATUS_ERROR);
        assertThat(baseServlet.getPingables().size()).isEqualTo(3);
    }

    @Test
    public void getApplicationVersion_from_environment() throws ServletException, IOException {
        assertThat(baseServlet.getApplicationVersion()).isEqualTo("unknown version");
        setProperty(APP_VERSION_PROPERTY_NAME, "123");
        assertThat(baseServlet.getApplicationVersion()).isEqualTo("123");
    }

    public static final int THREADS = 100;

    @Test(timeout = PING_TID * THREADS)
    public void beskyttPingablesMotMangeSamtidigeRequesterMenBevarHoyThroughput() throws ServletException, IOException {
        ExecutorService executorService = newFixedThreadPool(THREADS);
        range(0, 100)
                .mapToObj((i) -> executorService.submit(this::get))
                .collect(toList()).stream() // tvinger alle submits før vi resolver
                .forEach(SelfTestServletTest::resolveFuture);

        assertThat(pingA.counter.get()).isLessThan(5);
        assertThat(pingB.counter.get()).isLessThan(5);
        assertThat(pingC.counter.get()).isLessThan(5);
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
