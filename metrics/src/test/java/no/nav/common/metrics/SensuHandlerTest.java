package no.nav.common.metrics;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import org.json.JSONObject;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class SensuHandlerTest {

    private ServerSocket serverSocket;
    private SensuHandler sensuHandler;

    @Before
    public void setup() throws IOException {
        serverSocket = new ServerSocket(0);
    }

    @After
    public void cleanup() throws IOException {
        try {
            if (sensuHandler != null) {
                sensuHandler.shutdown();
            }
        } finally {
            serverSocket.close();
        }
    }

    @Test
    public void skriverJsonTilSocket() throws Exception {
        sensuHandler = TestUtil.sensuHandlerForTest(serverSocket.getLocalPort());
        sensuHandler.report("testOutput");
        JSONObject mottatt = new JSONObject(TestUtil.lesLinjeFraSocket(serverSocket));

        final JSONObject forventet = new JSONObject(
                "{\"status\":0,\"name\":\"testApp\",\"output\":\"testOutput\",\"type\":\"metric\",\"handlers\":[\"events_nano\"]}");
        assertTrue(mottatt.similar(forventet));
    }

    @Test
    public void senderDataPaNyNarSocketConnectionFeiler() throws Exception {
        AtomicInteger forsok = new AtomicInteger();
        CountDownLatch sendt = new CountDownLatch(1);
        sensuHandler = new SensuHandler(TestUtil.testConfig(serverSocket.getLocalPort())) {

            @Override
            void writeToSensu(JSONObject jsonObject, Socket socket) throws IOException {
                if (forsok.incrementAndGet() == 1) {
                    throw new IOException("dummy connection feil");
                }
                super.writeToSensu(jsonObject, socket);
                sendt.countDown();
            }
        };

        sensuHandler.report("testOutput");
        JSONObject mottatt = new JSONObject(TestUtil.lesLinjeFraSocket(serverSocket));
        assertTrue("Retry fullforte ikke socket-skrivingen", sendt.await(5, TimeUnit.SECONDS));

        final JSONObject forventet = new JSONObject(
                "{\"status\":0,\"name\":\"testApp\",\"output\":\"testOutput\",\"type\":\"metric\",\"handlers\":[\"events_nano\"]}");
        assertTrue(mottatt.similar(forventet));
        assertEquals(2, forsok.get());
    }

    @Test
    public void avslutterExecutorVedShutdown() throws Exception {
        sensuHandler = TestUtil.sensuHandlerForTest(serverSocket.getLocalPort());
        @SuppressWarnings("resource")
        ScheduledExecutorService executor = reporterExecutor();

        sensuHandler.shutdown();

        assertTrue("Executor er ikke avsluttet", executor.isTerminated());
    }

    @Test
    public void bevarerAvbruddOgStopperExecutorVedShutdown() throws Exception {
        sensuHandler = TestUtil.sensuHandlerForTest(serverSocket.getLocalPort());
        @SuppressWarnings("resource")
        ScheduledExecutorService executor = reporterExecutor();

        boolean avbrutt;
        Thread.currentThread().interrupt();
        try {
            sensuHandler.shutdown();

            assertTrue("Executor tar fortsatt imot oppgaver", executor.isShutdown());
        } finally {
            avbrutt = Thread.interrupted();
        }
        assertTrue("Avbruddsflagget ble fjernet", avbrutt);
        assertTrue("Executor er ikke avsluttet", executor.awaitTermination(5, TimeUnit.SECONDS));
    }

    private ScheduledExecutorService reporterExecutor() throws ReflectiveOperationException {
        var felt = SensuHandler.class.getDeclaredField("scheduledExecutorService");
        felt.setAccessible(true);
        return (ScheduledExecutorService) felt.get(sensuHandler);
    }

    @Test
    public void shouldCalculateCorrectBatchTime() {
        assertEquals(955, SensuHandler.calculateBatchTime(500, 10000, 100, 1000));
        assertEquals(100, SensuHandler.calculateBatchTime(10000, 10000, 100, 1000));
        assertEquals(1000, SensuHandler.calculateBatchTime(0, 10000, 100, 1000));
    }
}
