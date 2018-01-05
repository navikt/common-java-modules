package no.nav.metrics.handlers;

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

import static no.nav.metrics.MetricsClient.DISABLE_METRICS_REPORT;
import static no.nav.sbl.util.EnvironmentUtils.getOptionalProperty;

public class SensuHandler {

    public static final String SENSU_CLIENT_HOST = "sensu_client_host";
    public static final String SENSU_CLIENT_PORT = "sensu_client_port";

    private static final Logger logger = LoggerFactory.getLogger(SensuHandler.class);
    private static final int RETRY_INTERVAL = Integer.parseInt(System.getProperty("metrics.sensu.report.retryInterval", "1000"));
    private static final int QUEUE_SIZE = Integer.parseInt(System.getProperty("metrics.sensu.report.queueSize", "5000"));
    private static final int BATCHES_PER_SECOND = Integer.parseInt(System.getProperty("metrics.sensu.report.batchesPerSecond", "50"));
    private static final int BATCH_SIZE = Integer.parseInt(System.getProperty("metrics.sensu.report.batchSize", "100"));
    private static final int BATCH_DELAY = 1000 / BATCHES_PER_SECOND;

    private final LinkedBlockingQueue<String> reportQueue = new LinkedBlockingQueue<>(QUEUE_SIZE);
    private final String application;
    private final int sensuPort;
    private final String sensuHost;
    private long queueSisteGangFullTimestamp = 0;

    public SensuHandler(String application) {
        this.application = application;
        this.sensuHost = getOptionalProperty(SENSU_CLIENT_HOST).orElse("localhost");
        this.sensuPort = getOptionalProperty(SENSU_CLIENT_PORT).map(Integer::parseInt).orElse(3030); // System property'en sensu_client_port settes av plattformen

        if (!DISABLE_METRICS_REPORT) {
            logger.info("Metrics aktivert med parametre: batch size: {}, batches per second: {}, gir batch delay: {}ms, queue size: {}, retry interval: {}ms, sensu host {} sensu port: {}",
                    BATCH_SIZE,
                    BATCHES_PER_SECOND,
                    BATCH_DELAY,
                    QUEUE_SIZE,
                    RETRY_INTERVAL,
                    this.sensuHost,
                    this.sensuPort
            );
            ScheduledExecutorService scheduledExecutorService = Executors.newSingleThreadScheduledExecutor();
            scheduledExecutorService.execute(new SensuReporter());
        } else {
            logger.warn("metrics disabled");
        }
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
                    reportQueue.drainTo(reports, BATCH_SIZE - 1);
                    logger.debug("Sender {} metrikker", reports.size());

                    String influxOutput = StringUtils.join(reports, "\n");
                    JSONObject jsonObject = createJSON(influxOutput);

                    try (Socket socket = new Socket()) {
                        BufferedWriter writer = connectToSensu(socket);

                        writer.write(jsonObject.toString());
                        writer.newLine();
                        writer.flush();
                    } catch (IOException e) {
                        reportQueue.addAll(reports); // Legger tilbake i køen
                        logger.error("Noe gikk feil med tilkoblingen til Sensu socket: {} - {}",  e.getClass().getSimpleName(), e.getMessage());
                        Thread.sleep(RETRY_INTERVAL); // Unngår å spamme connections (og loggen med feilmeldinger) om noe ikke virker
                    }

                    Thread.sleep(BATCH_DELAY);

                } catch (InterruptedException e) {
                    logger.error("Å vente på neste objekt ble avbrutt, bør ikke kunne skje.", e);
                }

            }

        }

        private BufferedWriter connectToSensu(Socket socket) throws IOException {
            InetSocketAddress inetSocketAddress = new InetSocketAddress(sensuHost, sensuPort);
            socket.connect(inetSocketAddress, 500);
            return new BufferedWriter(new OutputStreamWriter(socket.getOutputStream()));
        }


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
