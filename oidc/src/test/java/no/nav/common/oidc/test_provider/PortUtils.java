package no.nav.common.oidc.test_provider;

import javax.net.ServerSocketFactory;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.util.Random;

public class PortUtils {

    private static final int MIN_PORT = 1024;

    private static final int MAX_PORT = 65535;

    private static Random random = new Random();


    public static int findAvailablePort() {
        return findAvailablePort(MIN_PORT, MAX_PORT);
    }

    private static int findRandomPort(int minPort, int maxPort) {
        int portRange = maxPort - minPort;
        return minPort + random.nextInt(portRange + 1);
    }

    private static boolean isPortAvailable(int port) {
        try {
            ServerSocket serverSocket = ServerSocketFactory.getDefault().createServerSocket(port, 1, InetAddress.getByName("localhost"));
            serverSocket.close();
            return true;
        } catch (Exception ignored) {
            return false;
        }
    }

    private static int findAvailablePort(int minPort, int maxPort) {
        int portRange = maxPort - minPort;
        int candidatePort;
        int searchCounter = 0;
        do {
            if (searchCounter > portRange) {
                throw new IllegalStateException("Could not find an available port");
            }
            candidatePort = findRandomPort(minPort, maxPort);
            searchCounter++;
        }
        while (!isPortAvailable(candidatePort));

        return candidatePort;
    }

}
