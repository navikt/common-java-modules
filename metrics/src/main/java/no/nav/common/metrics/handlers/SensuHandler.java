package no.nav.common.metrics.handlers;

import no.nav.common.metrics.MetricsConfig;
import org.apache.commons.lang3.StringUtils;
import org.json.JSONArray;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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

public class SensuHandler {

    private static final Logger logger = LoggerFactory.getLogger(SensuHandler.class);

    private final LinkedBlockingQueue<String> reportQueue;
    private final String application;
    private final int sensuPort;
    private final String sensuHost;
    private final ScheduledExecutorService scheduledExecutorService;
    private final int batchSize;
    private final int retryInterval;
    private final int batchDelay;

    private long queueSisteGangFullTimestamp = 0;

    public SensuHandler(MetricsConfig metricsConfig) {
        this.application = metricsConfig.getApplication();
        this.sensuHost = metricsConfig.getSensuHost();
        this.sensuPort = metricsConfig.getSensuPort();
        this.reportQueue = new LinkedBlockingQueue<>(metricsConfig.getQueueSize());
        this.batchDelay = 1000 / metricsConfig.getBatchesPerSecond();
        this.batchSize = metricsConfig.getBatchSize();
        this.retryInterval = metricsConfig.getRetryInterval();
        this.scheduledExecutorService = Executors.newSingleThreadScheduledExecutor();
        this.scheduledExecutorService.execute(new SensuReporter());

        logger.info("Metrics aktivert med parametre: {}", metricsConfig);
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
                    reportQueue.drainTo(reports, batchSize - 1);
                    logger.debug("Sender {} metrikker", reports.size());

                    String influxOutput = StringUtils.join(reports, "\n");
                    JSONObject jsonObject = createJSON(influxOutput);

                    try (Socket socket = new Socket()) {
                        writeToSensu(jsonObject, socket);
                    } catch (IOException e) {
                        reportQueue.addAll(reports); // Legger tilbake i køen
                        logger.error("Noe gikk feil med tilkoblingen til Sensu socket: {} - {}", e.getClass().getSimpleName(), e.getMessage());
                        Thread.sleep(retryInterval); // Unngår å spamme connections (og loggen med feilmeldinger) om noe ikke virker
                    }

                    Thread.sleep(batchDelay);

                } catch (InterruptedException e) {
                    logger.error("Å vente på neste objekt ble avbrutt, bør ikke kunne skje.", e);
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
        InetSocketAddress inetSocketAddress = new InetSocketAddress(sensuHost, sensuPort);
        socket.connect(inetSocketAddress, 500);
        return new BufferedWriter(new OutputStreamWriter(socket.getOutputStream()));
    }

    public void report(String output) {
        boolean result = reportQueue.offer(output); // Thread-safe måte å legge til i køen på, returnerer false i stedet for å blokkere om køen er full

        if (!result && (System.currentTimeMillis() - queueSisteGangFullTimestamp > 1000 * 60)) { // Unngår å spamme loggen om køen er full over lengre tid (f. eks. Sensu er nede)
            logger.warn("Sensu-køen har vært full, ikke alle metrikker har blitt sendt til Sensu.");
            queueSisteGangFullTimestamp = System.currentTimeMillis();
        }
    }

    private JSONObject createJSON(String output) {
        JSONObject jsonObject = new JSONObject();
        jsonObject.put("name", application);
        jsonObject.put("type", "metric");
        jsonObject.put("output", output);
        jsonObject.put("status", 0);
        jsonObject.put("handlers", new JSONArray("[events_nano]"));

        return jsonObject;
    }
}
