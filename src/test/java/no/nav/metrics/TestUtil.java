package no.nav.metrics;

import org.springframework.aop.aspectj.annotation.AspectJProxyFactory;

import java.io.*;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.net.ServerSocket;
import java.net.Socket;

public class TestUtil {

    public static <T> T lagAspectProxy(T target, Object aspect) {
        AspectJProxyFactory factory = new AspectJProxyFactory(target);
        factory.addAspect(aspect);
        return factory.getProxy();
    }

    public static void resetMetrics() {
        try {
            // Lukk øynene
            Field metricsClient = MetricsFactory.class.getDeclaredField("metricsClient");
            metricsClient.setAccessible(true);

            // Fjerner final lol
            Field modifier = Field.class.getDeclaredField("modifiers");
            modifier.setAccessible(true);
            modifier.setInt(metricsClient, metricsClient.getModifiers() & ~Modifier.FINAL);

            metricsClient.set(null, new MetricsClient());
        } catch (NoSuchFieldException | IllegalAccessException e) {
            throw new RuntimeException(e);
        }

    }

    public static String lesLinjeFraSocket(ServerSocket serverSocket) throws IOException {
        Socket socket = serverSocket.accept();
        BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
        return bufferedReader.readLine();
    }

    public static int getSensuClientPort() {
        return Integer.parseInt(System.getProperty("sensu_client_port", "3030"));
    }
}
