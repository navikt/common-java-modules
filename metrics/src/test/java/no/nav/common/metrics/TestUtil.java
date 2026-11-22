package no.nav.common.metrics;

import static no.nav.common.metrics.SensuConfig.DEFAULT_SENSU_BATCH_SIZE;
import static no.nav.common.metrics.SensuConfig.DEFAULT_SENSU_CONNECT_TIMEOUT;
import static no.nav.common.metrics.SensuConfig.DEFAULT_SENSU_QUEUE_SIZE;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import org.json.JSONObject;

public class TestUtil {

    public static List<String> lesUtAlleMeldingerSendtPaSocket(ServerSocket serverSocket, int antallMeldinger)
            throws IOException {
        List<String> meldinger = new ArrayList<>();
        long frist = System.nanoTime() + TimeUnit.SECONDS.toNanos(5);
        while (meldinger.size() < antallMeldinger) {
            meldinger.addAll(splitStringsFraMelding(lesLinjeFraSocket(serverSocket, frist)));
        }
        return meldinger;
    }

    public static String lesLinjeFraSocket(ServerSocket serverSocket) throws IOException {
        return lesLinjeFraSocket(serverSocket, System.nanoTime() + TimeUnit.SECONDS.toNanos(5));
    }

    private static String lesLinjeFraSocket(ServerSocket serverSocket, long frist) throws IOException {
        serverSocket.setSoTimeout(gjenstaendeMillis(frist));
        try (Socket socket = serverSocket.accept();
                BufferedReader reader = new BufferedReader(new InputStreamReader(socket.getInputStream()))) {
            socket.setSoTimeout(gjenstaendeMillis(frist));
            String linje = reader.readLine();
            if (linje == null) {
                throw new IOException("Socketen ble lukket uten en melding");
            }
            return linje;
        }
    }

    private static int gjenstaendeMillis(long frist) throws SocketTimeoutException {
        long gjenstaende = frist - System.nanoTime();
        if (gjenstaende <= 0) {
            throw new SocketTimeoutException("Mottok ikke forventede meldinger innen fem sekunder");
        }
        return (int) Math.max(1, TimeUnit.NANOSECONDS.toMillis(gjenstaende));
    }

    public static List<String> splitStringsFraMelding(String melding) {
        return List.of(new JSONObject(melding).getString("output").split("\n"));
    }

    public static SensuHandler sensuHandlerForTest(int port) {
        return new SensuHandler(testConfig(port));
    }

    public static SensuConfig testConfig(int port) {
        return SensuConfig.builder()
                .sensuHost("localhost")
                .sensuPort(port)
                .application("testApp")
                .hostname("test-hostname")
                .cluster("test-cluster")
                .namespace("test-namespace")
                .retryInterval(100)
                .connectTimeout(DEFAULT_SENSU_CONNECT_TIMEOUT)
                .queueSize(DEFAULT_SENSU_QUEUE_SIZE)
                .maxBatchTime(200)
                .batchSize(DEFAULT_SENSU_BATCH_SIZE)
                .cleanupOnShutdown(false)
                .build();
    }
}
