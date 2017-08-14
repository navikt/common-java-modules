package no.nav.metrics.integration;

import no.nav.metrics.MetricsFactory;
import no.nav.metrics.TestUtil;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;
import java.net.ServerSocket;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static no.nav.metrics.TestUtil.getSensuClientPort;
import static no.nav.metrics.TestUtil.lesLinjeFraSocket;
import static no.nav.metrics.handlers.SensuHandler.SENSU_CLIENT_PORT;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.lessThan;
import static org.junit.Assert.assertThat;

public class RaceConditionTest {

    private ServerSocket serverSocket;

    @Before
    public void setup() throws IOException {
        serverSocket = new ServerSocket(getSensuClientPort());
        System.setProperty(SENSU_CLIENT_PORT, Integer.toString(serverSocket.getLocalPort()));
    }

    @Test
    public void testStuff() throws Exception {
        TestUtil.resetMetrics();
        Thread.sleep(100);

        /*

        Denne koden/analysen ble skrevet for å verifisere en race-condition vi opplevde under ytelsestest.
        Skrevet den om til en test da den kan gi verdi for å verifisere at koden i fremtiden er korrekt

        Lager en metode som simulerer arbeid som tar litt tid, wrapper den i en TimerProxy.
        Sørger for at kallene blir ca. slik, altså at kall nr2 starter og fullfører mens kall nr1 jobber

              |----- 100 ----- 150 ----- 200 ----- 250 ----- 300 ----- 350 ----- 400 -----
        nr1   | sleep | start                                                     | done
        nr2   | sleep           | start              | done

        dette fører til at nr2 reporter metrics og resetter timeren, samme timer som nr1 bruker, så når en er ferdig blir alt crazy.


        Slik som det er i dag:
        FEIL, mangler success=true på nr 2 samt bogus value
        {"status":0,"output":"testProxy.doStuff.timer,application=null,environment=null,hostname=null value=50,success=true 1473090375947000000","type":"metric","handlers":["events_nano"]}
        {"status":0,"output":"testProxy.doStuff.timer,application=null,environment=null,hostname=null value=1047628109 0","type":"metric","handlers":["events_nano"]}

        Kan se at tidene (stoptime / starttime) i Timer er rare:
        1047627905790261 / 1047627855037442
        1047628109789931 / 0


        Med "synchronized" på invoke-metoden i MetricProxy:
        OK:
        {"status":0,"output":"testProxy.doStuff.timer,application=null,environment=null,hostname=null value=300,success=true 1473089765355000000","type":"metric","handlers":["events_nano"]}
        {"status":0,"output":"testProxy.doStuff.timer,application=null,environment=null,hostname=null value=50,success=true 1473089765683000000","type":"metric","handlers":["events_nano"]}

        Men det knekker jo all multithreading/ytelse

        */


        TimeMeImpl timeMe = new TimeMeImpl();
        final TimeMe proxy = MetricsFactory.createTimerProxy("testProxy", timeMe, TimeMe.class);

        ExecutorService ex = Executors.newFixedThreadPool(2);

        // nr 1
        ex.execute(new Runnable() {
            @Override
            public void run() {
                try {
                    Thread.sleep(100);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                proxy.doStuff(300);
            }
        });

        // nr2
        ex.execute(new Runnable() {
            @Override
            public void run() {
                try {
                    Thread.sleep(150);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                proxy.doStuff(100);
            }
        });


        long start = System.currentTimeMillis();

        String line1 = lesLinjeFraSocket(serverSocket);
        String line2 = lesLinjeFraSocket(serverSocket);

        int timeUsed = (int) (System.currentTimeMillis() - start);

        assertThat("skal ikke kjøres serielt", timeUsed, lessThan(500));
        assertThat(line1, containsString("success=true"));
        assertThat(line1, containsString("testProxy.doStuff"));
        assertThat(line2, containsString("success=true"));
        assertThat(line2, containsString("testProxy.doStuff"));

        serverSocket.close();
    }

    @SuppressWarnings("WeakerAccess")
    public interface TimeMe {
        void doStuff(int sleep);
    }

    private static class TimeMeImpl implements TimeMe {
        @Override
        public void doStuff(int sleep) {
            try {
                Thread.sleep(sleep);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }
}