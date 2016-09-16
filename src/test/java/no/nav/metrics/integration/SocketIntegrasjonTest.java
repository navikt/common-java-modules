package no.nav.metrics.integration;

import no.nav.metrics.handlers.SensuHandler;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static org.hamcrest.core.StringContains.containsString;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;

public class SocketIntegrasjonTest {
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
                new SensuHandler().report("testApp", "data123");
            }
        }).start();

        ServerSocket serverSocket = new ServerSocket(3030);
        Socket socket = serverSocket.accept();

        BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
        assertThat(bufferedReader.readLine(), containsString("data123"));

        serverSocket.close();
    }
    private static final Logger logger = LoggerFactory.getLogger(SensuHandler.class);

    @Test
    public void proverPaNyttOmFeiler() throws Exception {
        new SensuHandler().report("testApp", "data123");

        Thread.sleep(600); // Venter med å lage socket en stund
        ServerSocket serverSocket = new ServerSocket(3030);

        Socket socket = serverSocket.accept();
        BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
        String output = bufferedReader.readLine();

        if (output == null) {
            socket = serverSocket.accept();
            bufferedReader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            output = bufferedReader.readLine();
        }

        assertThat(output, containsString("data123"));

        serverSocket.close();
    }

    @Test
    public void taklerMyeLast() throws Exception {
        ExecutorService ex = Executors.newFixedThreadPool(4);

        final SensuHandler sensuHandler = new SensuHandler();

        for (int i = 0; i < 100; i++) {
            final int finalI = i;
            ex.execute(new Runnable() {
                @Override
                public void run() {
                    sensuHandler.report("test", "nr " + finalI);
                }
            });
        }

        ServerSocket serverSocket = new ServerSocket(3030);
        Socket socket = serverSocket.accept();

        BufferedReader reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));

        // Kan lese ut riktig antall meldinger fra socketen uten at testen henger
        int antallLinjer = 0;
        for (int i = 0; i < 100; i++) {
            reader.readLine();
            antallLinjer++;
        }

        assertEquals(100, antallLinjer);

        serverSocket.close();
    }
}
