package no.nav.common.metrics;

import static no.nav.common.metrics.TestUtil.lesLinjeFraSocket;
import static no.nav.common.metrics.TestUtil.lesUtAlleMeldingerSendtPaSocket;
import static no.nav.common.metrics.TestUtil.sensuHandlerForTest;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.StringContains.containsString;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import org.json.JSONObject;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class SocketIntegrasjonTest {

    private ServerSocket serverSocket;
    private SensuHandler sensuHandler;

    @Before
    public void setup() throws IOException {
        serverSocket = new ServerSocket(0);
        sensuHandler = sensuHandlerForTest(serverSocket.getLocalPort());
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
    public void senderJsonOverSocket() throws Exception {
        sensuHandler.report("data123");

        String linje = lesLinjeFraSocket(serverSocket);

        assertThat(linje, containsString("data123"));
    }

    @Test
    public void proverPaNyttOmFeiler() throws Exception {
        sensuHandler.shutdown();
        AtomicInteger forsok = new AtomicInteger();
        sensuHandler = new SensuHandler(TestUtil.testConfig(serverSocket.getLocalPort())) {
            @Override
            void writeToSensu(JSONObject jsonObject, Socket socket) throws IOException {
                if (forsok.incrementAndGet() == 1) {
                    throw new IOException("Fremprovosert tilkoblingsfeil");
                }
                super.writeToSensu(jsonObject, socket);
            }
        };
        sensuHandler.report("data567");

        String linje = lesLinjeFraSocket(serverSocket);
        assertThat(linje, containsString("data567"));
        assertEquals(2, forsok.get());
    }

    @Test
    public void taklerMyeLast() throws Exception {
        @SuppressWarnings("resource")
        ExecutorService ex = Executors.newFixedThreadPool(4);
        int antallTester = 500;

        try {
            List<Future<?>> oppgaver = new ArrayList<>();
            for (int i = 0; i < antallTester; i++) {
                final int finalI = i;
                oppgaver.add(ex.submit(() -> sensuHandler.report("nr" + finalI)));
            }
            ex.shutdown();
            assertTrue("Rapportering ble ikke ferdig", ex.awaitTermination(5, TimeUnit.SECONDS));
            for (Future<?> oppgave : oppgaver) {
                oppgave.get(5, TimeUnit.SECONDS);
            }

            List<String> meldinger = lesUtAlleMeldingerSendtPaSocket(serverSocket, antallTester);
            assertEquals(antallTester, meldinger.size());
            for (int i = 0; i < antallTester; i++) {
                if (!meldinger.contains("nr" + i)) {
                    fail("Fant ikke melding nr" + i);
                }
            }
        } finally {
            ex.shutdownNow();
            assertTrue("Test-executor er ikke avsluttet", ex.awaitTermination(5, TimeUnit.SECONDS));
        }
    }
}
