package no.nav.metrics.handlers;

import org.json.JSONArray;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ScheduledExecutorService;

import static no.nav.metrics.MetricsClient.DISABLE_METRICS_REPORT;

public class SensuHandler {

    private static final Logger logger = LoggerFactory.getLogger(SensuHandler.class);
    private static final int SENSU_PORT = Integer.parseInt(System.getProperty("sensu_client_port", "3030"));

    private final LinkedBlockingQueue<JSONObject> reportQueue = new LinkedBlockingQueue<>(1000);

    public SensuHandler() {
        if (!DISABLE_METRICS_REPORT) {
            ScheduledExecutorService scheduledExecutorService = Executors.newSingleThreadScheduledExecutor();
            scheduledExecutorService.execute(new SensuReporter());
        }
    }

    private class SensuReporter implements Runnable {

        @Override
        public void run() {

            while (true) {
                try {
                    JSONObject object = reportQueue.take(); // denne venter til det er noe i køen

                    try (Socket socket = new Socket()) {
                        BufferedWriter writer = connectToSensu(socket);

                        writer.write(object.toString());
                        writer.newLine();
                        writer.flush();
                    } catch (IOException e) {
                        reportQueue.offer(object);
                        logger.error("Noe gikk feil med tilkoblingen til Sensu socket", e);
                        Thread.sleep(1000); // Unngår å spamme connections (og loggen med feilmeldinger) om noe ikke virker
                    }
                } catch (InterruptedException e) {
                    logger.error("Å vente på neste objekt ble avbrutt, bør ikke kunne skje", e);
                }

            }

        }

        private BufferedWriter connectToSensu(Socket socket) throws IOException {
            InetSocketAddress inetSocketAddress = new InetSocketAddress("localhost", SENSU_PORT);
            socket.connect(inetSocketAddress, 500);
            return new BufferedWriter(new OutputStreamWriter(socket.getOutputStream()));
        }


    }

    public void report(String application, String output) {
        JSONObject json = createJSON(application, output);
        boolean result = reportQueue.offer(json);// blir ikke lagt til om ikke plass, men må få tak i en lock...

        if (!result) {
            logger.info("Sensu-køen er full");
        }
    }

    public static JSONObject createJSON(String application, String output) {
        JSONObject jsonObject = new JSONObject();
        jsonObject.put("name", application);
        jsonObject.put("type", "metric");
        jsonObject.put("output", output);
        jsonObject.put("status", 0);
        jsonObject.put("handlers", new JSONArray("[events_nano]"));

        return jsonObject;
    }
}
