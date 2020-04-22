package no.nav.common.metrics;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;
import java.net.ServerSocket;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static no.nav.common.metrics.TestUtil.*;
import static org.hamcrest.core.StringContains.containsString;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;

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
        serverSocket.close();
    }

    @Test
    public void senderJsonOverSocket() throws Exception {
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    Thread.sleep(50); // Vente på socketen er klar
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                sensuHandler.report("data123");
            }
        }).start();


        String linje = lesLinjeFraSocket(serverSocket);

        assertThat(linje, containsString("data123"));
    }

    @Test
    public void proverPaNyttOmFeiler() throws Exception {
        sensuHandler.report("data567");

        Thread.sleep(600); // Venter med å lage socket en stund

        String linje = lesLinjeFraSocket(serverSocket);

        if (linje == null) {
            System.out.println("Måtte prøve på nytt");
            lesLinjeFraSocket(serverSocket);
        }

        assertThat(linje, containsString("data567"));
    }

    @Test
    public void taklerMyeLast() throws Exception {
        ExecutorService ex = Executors.newFixedThreadPool(4);
        int antallTester = 500;

        for (int i = 0; i < antallTester; i++) {
            final int finalI = i;
            ex.execute(new Runnable() {
                @Override
                public void run() {
                    sensuHandler.report("nr" + finalI);
                }
            });
        }

        // Kan lese ut alle meldingene igjen
        List<String> meldinger = lesUtAlleMeldingerSendtPaSocket(serverSocket);

        for (int i = 0; i < antallTester; i++) {
            boolean contains = meldinger.contains("nr" + i);
            if (!contains) {
                fail("Fant ikke melding nr" + i);
            }
        }
    }
}
