package no.nav.metrics;

import no.nav.metrics.handlers.SensuHandler;
import org.springframework.aop.aspectj.annotation.AspectJProxyFactory;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static java.lang.System.setProperty;
import static no.nav.sbl.util.EnvironmentUtils.APP_NAME_PROPERTY_NAME;
import static no.nav.sbl.util.EnvironmentUtils.FASIT_ENVIRONMENT_NAME_PROPERTY_NAME;

public class TestUtil {

    public static <T> T lagAspectProxy(T target, Object aspect) {
        AspectJProxyFactory factory = new AspectJProxyFactory(target);
        factory.addAspect(aspect);
        return factory.getProxy();
    }

    public static List<String> lesUtAlleMeldingerSendtPaSocket(ServerSocket serverSocket) throws IOException {
        List<String> meldinger = new ArrayList<>();

        String linje = lesLinjeFraSocket(serverSocket);
        while (linje != null) {
            meldinger.addAll(splitStringsFraMelding(linje));
            linje = lesLinjeFraSocket(serverSocket);
        }

        return meldinger;
    }


    public static String lesLinjeFraSocket(ServerSocket serverSocket) throws IOException {
        try {
            serverSocket.setSoTimeout(2000);
            Socket socket = serverSocket.accept();
            BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            return bufferedReader.readLine();
        } catch (SocketTimeoutException e) {
            return null;
        }
    }

    public static List<String> splitStringsFraMelding(String melding) {

        int start = melding.indexOf("output\":\"") + 9;
        int end = melding.indexOf("\"", start + 1);

        String output = melding.substring(start, end);

        return Arrays.asList(output.split("\\\\n"));
    }

    public static void enableMetricsForTest(int port) {
        setProperty(APP_NAME_PROPERTY_NAME, "testApp");
        setProperty(FASIT_ENVIRONMENT_NAME_PROPERTY_NAME, "T42");
        MetricsClient.resetMetrics(new MetricsConfig("localhost", port));
    }

    public static SensuHandler sensuHandlerForTest(int port) {
        return new SensuHandler("testApp", new MetricsConfig("localhost", port));
    }
}
