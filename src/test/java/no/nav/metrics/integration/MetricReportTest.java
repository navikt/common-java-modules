package no.nav.metrics.integration;

import no.nav.metrics.MetricsFactory;
import no.nav.metrics.TestUtil;
import no.nav.metrics.aspects.Timed;
import no.nav.metrics.aspects.TimerAspect;
import org.junit.Test;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.ServerSocket;
import java.net.Socket;

import static org.junit.Assert.assertEquals;

public class MetricReportTest {

    @Test
    public void aspectOgProxySkalRapportereLikeData() throws Exception {
        TestUtil.resetMetrics();

        final TimeMe timerProxy = MetricsFactory.createTimerProxy("TimeMe", new TimeMeImpl(), TimeMe.class);

        final TimeMe timerAspect = TestUtil.lagAspectProxy(new TimeMeImpl(), new TimerAspect());

        new Thread(new Runnable() {
            @Override
            public void run() {
                timerProxy.time();
                timerAspect.time();
            }
        }).start();

        ServerSocket serverSocket = new ServerSocket(3030);
        Socket socket = serverSocket.accept();

        BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
        String line1 = bufferedReader.readLine();
        String line2 = bufferedReader.readLine();

        assertEquals(fjernTimestamps(line1), fjernTimestamps(line2));

        serverSocket.close();
    }

    private String fjernTimestamps(String data) {
        return data
                .replaceAll("value=\\d+", "value=<dummy>")
                .replaceAll("\\d{19}", "<timestamp>");
    }


    public interface TimeMe {
        void time();
    }

    public static class TimeMeImpl implements TimeMe {
        @Timed
        @Override
        public void time() {

        }
    }

}
