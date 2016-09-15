package no.nav.metrics.debug;

import no.nav.metrics.MetricsFactory;
import no.nav.metrics.TestUtil;
import org.junit.Test;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.lessThan;
import static org.junit.Assert.assertThat;

public class RaceConditionTest {

    @Test
    public void testStuff() throws Exception {
        TestUtil.resetMetrics();
        /*

        Lager en metode som simulerer arbeid som tar litt tid, wrapper den i en TimerProxy.
        Sørger for at kallene blir ca. slik, altså at kall nr2 starter og fullfører mens kall nr1 jobber

              |----- 100 ----- 150 ----- 200 ----- 250 ----- 300 ----- 350 ----- 400 -----
        nr1   | sleep | start                                                     | done
        nr2   | sleep           | start              | done

        dette fører til at nr2 reporter metrics og resetter timeren, samme timer som nr1 bruker, så når en er ferdig blir alt crazy.



        Start ncat lokalt og kjør testen
        ncat -l -k 3030
        evt. med bare "nc" i stedet for "ncat"

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

        ServerSocket serverSocket = new ServerSocket(3030);

        Socket socket = serverSocket.accept();
        BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
        String line1 = bufferedReader.readLine();
        String line2 = bufferedReader.readLine();


        int timeUsed = (int) (System.currentTimeMillis() - start);

        assertThat("skal ikke kjøres serielt", timeUsed, lessThan(500));
        assertThat(line1, containsString("success=true"));
        assertThat(line2, containsString("success=true"));

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