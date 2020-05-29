package no.nav.common.metrics;

import lombok.extern.slf4j.Slf4j;
import no.nav.common.utils.MathUtils;
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
import java.util.concurrent.TimeUnit;

import static java.lang.String.format;
import static no.nav.common.metrics.SensuConfig.SENSU_MIN_BATCH_TIME;
import static no.nav.common.metrics.SensuConfig.SENSU_MIN_QUEUE_SIZE;

@Slf4j
public class SensuHandler {

    private final SensuConfig sensuConfig;

    private final LinkedBlockingQueue<String> reportQueue;

    private final ScheduledExecutorService scheduledExecutorService;

    private volatile long queueSisteGangFullTimestamp;

    private volatile boolean isShutDown;

    public SensuHandler(SensuConfig sensuConfig) {
        validateSensuConfig(sensuConfig);

        this.sensuConfig = sensuConfig;
        this.reportQueue = new LinkedBlockingQueue<>(sensuConfig.getQueueSize());
        this.scheduledExecutorService = Executors.newSingleThreadScheduledExecutor();
        this.scheduledExecutorService.execute(new SensuReporter());

        if (sensuConfig.isCleanupOnShutdown()) {
            setupShutdownHook();
        }

        log.info("Metrics aktivert med parametre: {}", sensuConfig);
    }

    private void setupShutdownHook() {
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            if (isShutDown) {
                log.info("Sensu is already shut down. Skipping shutdown hook.");
                return;
            }

            shutdown();
            log.info("Sensu shutdown hook triggered. Will try to flush leftover metrics.");

            if (!reportQueue.isEmpty()) {
                List<String> reports = new ArrayList<>();
                reportQueue.drainTo(reports, Integer.MAX_VALUE);
                String influxOutput = StringUtils.join(reports, "\n");
                JSONObject jsonObject = createJSON(influxOutput);

                try (Socket socket = new Socket()) {
                    writeToSensu(jsonObject, socket);
                    log.info("Successfully flushed metrics after shutdown. Metrics sent: " + reports.size());
                } catch (Exception e) {
                    log.error("Failed to flush metrics after shutdown. Metrics that were not sent: " + reports.size(), e);
                }
            }
        }));
    }

    public void shutdown() {
        isShutDown = true;
        try {
            scheduledExecutorService.awaitTermination(5, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    private class SensuReporter implements Runnable {

        @Override
        public void run() {

            List<String> reports = new ArrayList<>();

            while (!isShutDown) {
                try {
                    if (!reportQueue.isEmpty()) {
                        reports.clear();

                        reportQueue.drainTo(reports, sensuConfig.getBatchSize() - 1);
                        float percentFull = (reportQueue.size() / (float) sensuConfig.getQueueSize()) * 100;
                        log.info(format("Metrics still in queue: %d \tQueue full: %.2f%% \tMetrics sent: %d", reportQueue.size(), percentFull, reports.size()));

                        String influxOutput = StringUtils.join(reports, "\n");
                        JSONObject jsonObject = createJSON(influxOutput);

                        try (Socket socket = new Socket()) {
                            writeToSensu(jsonObject, socket);
                        } catch (IOException e) {
                            reportQueue.addAll(reports); // Legger tilbake i køen
                            log.error("Noe gikk feil med tilkoblingen til Sensu socket: {} - {}", e.getClass().getSimpleName(), e.getMessage());
                            if (!isShutDown) {
                                Thread.sleep(sensuConfig.getRetryInterval()); // Unngår å spamme connections (og loggen med feilmeldinger) om noe ikke virker
                            }
                        }
                    }

                    if (!isShutDown) {
                        long batchTime = calculateBatchTime(reportQueue.size(), sensuConfig.getQueueSize(), SENSU_MIN_BATCH_TIME, sensuConfig.getMaxBatchTime());
                        Thread.sleep(batchTime);
                    }
                } catch (InterruptedException e) {
                    log.error("Å vente på neste objekt ble avbrutt, bør ikke kunne skje", e);
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

    /**
     * Calculates batch time based on how full the queue is. Fuller queue = less time between batches
     */
    static long calculateBatchTime(int currentQueueSize, int maxQueueSize, long minBatchTime, long maxBatchTime) {
        float percentEmpty = 1 - (currentQueueSize / (float) maxQueueSize);
        return MathUtils.linearInterpolation(minBatchTime, maxBatchTime, percentEmpty);
    }

    private void validateSensuConfig(SensuConfig sensuConfig) {
        if (sensuConfig.getMaxBatchTime() < SENSU_MIN_BATCH_TIME) {
            throw new IllegalArgumentException("Max batch time must be equal to or above " + SENSU_MIN_BATCH_TIME);
        } else if (sensuConfig.getQueueSize() < SENSU_MIN_QUEUE_SIZE) {
            throw new IllegalArgumentException("Queue size must be equal to or larger than " + SENSU_MIN_QUEUE_SIZE);
        }
    }

    public void report(String output) {
        if (isShutDown) {
            log.error("Cannot send report to sensu after shutting down");
            return;
        }

        boolean result = reportQueue.offer(output); // Thread-safe måte å legge til i køen på, returnerer false i stedet for å blokkere om køen er full

        if (!result && (System.currentTimeMillis() - queueSisteGangFullTimestamp > 1000 * 60)) { // Unngår å spamme loggen om køen er full over lengre tid (f. eks. Sensu er nede)
            log.error("Sensu-køen har vært full, ikke alle metrikker har blitt sendt til Sensu");
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
