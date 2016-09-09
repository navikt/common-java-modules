package no.nav.metrics.handlers;

import org.json.JSONObject;
import org.junit.Test;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static no.nav.metrics.handlers.SensuHandler.createJSON;
import static org.hamcrest.core.StringContains.containsString;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;

public class SensuHandlerTest {

    @Test
    public void createJSONSetsTypeToMetric() {
        JSONObject jsonObject = createJSON("test", "test");

        assertEquals("metric", jsonObject.get("type"));
    }

    @Test
    public void createJSONSetsStatusToZero() {
        JSONObject jsonObject = createJSON("test", "test");

        assertEquals(0, jsonObject.get("status"));
    }

    @Test
    public void createJSONSetsHandlersToEvents() {
        JSONObject jsonObject = createJSON("test", "test");
        String handlers = jsonObject.get("handlers").toString();

        assertEquals("[\"events_nano\"]", handlers);
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
                new SensuHandler().report("testApp", "data123");
            }
        }).start();

        ServerSocket serverSocket = new ServerSocket(3030);
        Socket socket = serverSocket.accept();

        BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
        assertThat(bufferedReader.readLine(), containsString("data123"));

        serverSocket.close();
    }

    @Test
    public void proverPaNyttOmFeiler() throws Exception {

        new Thread(new Runnable() {
            @Override
            public void run() {
                new SensuHandler().report("testApp", "data123");
            }
        }).start();

        Thread.sleep(1000); // Venter med å lage socket en stund
        ServerSocket serverSocket = new ServerSocket(3030);
        Socket socket = serverSocket.accept();

        BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
        assertThat(bufferedReader.readLine(), containsString("data123"));

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