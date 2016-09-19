package no.nav.metrics.integration;

import no.nav.metrics.handlers.SensuHandler;
import org.junit.Test;

import java.net.ServerSocket;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static no.nav.metrics.TestUtil.lesLinjeFraSocket;
import static org.hamcrest.core.StringContains.containsString;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;

public class SocketIntegrasjonTest {
    @Test
    public void senderJsonOverSocket() throws Exception {
        Thread.sleep(100);

        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    Thread.sleep(50); // Vente på socketen er klar
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                new SensuHandler().report("testApp", "data123");
            }
        }).start();

        ServerSocket serverSocket = new ServerSocket(3030);

        String linje = lesLinjeFraSocket(serverSocket);

        assertThat(linje, containsString("data123"));

        serverSocket.close();
    }

    @Test
    public void proverPaNyttOmFeiler() throws Exception {
        Thread.sleep(100);

        new SensuHandler().report("testApp", "data123");

        Thread.sleep(600); // Venter med å lage socket en stund
        ServerSocket serverSocket = new ServerSocket(3030);

        String linje = lesLinjeFraSocket(serverSocket);

        if (linje == null) {
            System.out.println("Måtte prøve på nytt");
            lesLinjeFraSocket(serverSocket);
        }

        assertThat(linje, containsString("data123"));

        serverSocket.close();
    }

    @Test
    public void taklerMyeLast() throws Exception {
        Thread.sleep(100);
        ExecutorService ex = Executors.newFixedThreadPool(4);
        final SensuHandler sensuHandler = new SensuHandler();
        int antallTester = 500;

        for (int i = 0; i < antallTester; i++) {
            final int finalI = i;
            ex.execute(new Runnable() {
                @Override
                public void run() {
                    sensuHandler.report("test", "nr " + finalI);
                }
            });
        }

        ServerSocket serverSocket = new ServerSocket(3030);

        // Kan lese ut riktig antall meldinger fra socketen uten at testen henger
        int antallLinjer = 0;
        for (int i = 0; i < antallTester; i++) {
            if (lesLinjeFraSocket(serverSocket) == null) {
                break;
            }
            antallLinjer++;
        }

        assertEquals(antallTester, antallLinjer);
        serverSocket.close();
    }
}
