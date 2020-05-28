package no.nav.common.metrics;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ScheduledExecutorService;

@Slf4j
public class SensuHandler {

    private final SensuConfig sensuConfig;

    private final LinkedBlockingQueue<String> reportQueue;

    private final ScheduledExecutorService scheduledExecutorService;

    private long queueSisteGangFullTimestamp = 0;

    public SensuHandler(SensuConfig sensuConfig) {
        this.sensuConfig = sensuConfig;
        this.reportQueue = new LinkedBlockingQueue<>(sensuConfig.getQueueSize());
        this.scheduledExecutorService = Executors.newSingleThreadScheduledExecutor();
        this.scheduledExecutorService.execute(new SensuReporter());
        log.info("Metrics aktivert med parametre: {}", sensuConfig);
    }

    public void shutdown() {
        scheduledExecutorService.shutdown();
    }

    private class SensuReporter implements Runnable {

        @Override
        public void run() {

            List<String> reports = new ArrayList<>();

            while (true) {
                try {
                    String report = reportQueue.take(); // denne venter til det er noe i køen

                    reports.clear();
                    reports.add(report);
                    reportQueue.drainTo(reports, sensuConfig.getBatchSize() - 1);
                    log.debug("Sender {} metrikker", reports.size());

                    String influxOutput = StringUtils.join(reports, "\n");
                    JSONObject jsonObject = createJSON(influxOutput);

                    try (Socket socket = new Socket()) {
                        writeToSensu(jsonObject, socket);
                    } catch (IOException e) {
                        reportQueue.addAll(reports); // Legger tilbake i køen
                        log.error("Noe gikk feil med tilkoblingen til Sensu socket: {} - {}", e.getClass().getSimpleName(), e.getMessage());
                        Thread.sleep(sensuConfig.getRetryInterval()); // Unngår å spamme connections (og loggen med feilmeldinger) om noe ikke virker
                    }

                    Thread.sleep(sensuConfig.getBatchTime());

                } catch (InterruptedException e) {
                    log.error("Å vente på neste objekt ble avbrutt, bør ikke kunne skje.", e);
                }

            }

        }
    }

    void writeToSensu(JSONObject jsonObject, Socket socket) throws IOException {
        BufferedWriter writer = connectToSensu(socket);
        writer.write(jsonObject.toString());
        writer.newLine();
        writer.flush();
    }

    private BufferedWriter connectToSensu(Socket socket) throws IOException {
        InetSocketAddress inetSocketAddress = new InetSocketAddress(sensuConfig.getSensuHost(), sensuConfig.getSensuPort());
        socket.connect(inetSocketAddress, sensuConfig.getConnectTimeout());
        return new BufferedWriter(new OutputStreamWriter(socket.getOutputStream()));
    }

    public void report(String output) {
        boolean result = reportQueue.offer(output); // Thread-safe måte å legge til i køen på, returnerer false i stedet for å blokkere om køen er full

        if (!result && (System.currentTimeMillis() - queueSisteGangFullTimestamp > 1000 * 60)) { // Unngår å spamme loggen om køen er full over lengre tid (f. eks. Sensu er nede)
            log.error("Sensu-køen har vært full, ikke alle metrikker har blitt sendt til Sensu.");
            queueSisteGangFullTimestamp = System.currentTimeMillis();
        }
    }

    private JSONObject createJSON(String output) {
        JSONObject jsonObject = new JSONObject();
        jsonObject.put("name", sensuConfig.getApplication());
        jsonObject.put("type", "metric");
        jsonObject.put("output", output);
        jsonObject.put("status", 0);
        jsonObject.put("handlers", new JSONArray("[events_nano]"));

        return jsonObject;
    }
}
