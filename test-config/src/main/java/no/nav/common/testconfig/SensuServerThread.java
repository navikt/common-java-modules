package no.nav.common.testconfig;

import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.ServerSocket;
import java.net.Socket;

@Slf4j
public class SensuServerThread extends Thread {

    private final ServerSocket serverSocket;

    @SneakyThrows
    SensuServerThread() {
        setDaemon(true);
        serverSocket = new ServerSocket(0);
    }

    public int getPort(){
        return serverSocket.getLocalPort();
    }

    @Override
    public void run() {
        while (true) {
            try {
                Socket accept = serverSocket.accept();
                try (InputStreamReader inputStreamReader = new InputStreamReader(accept.getInputStream())) {
                    try (BufferedReader bufferedReader = new BufferedReader(inputStreamReader)) {
                        log.info(bufferedReader.readLine());
                    }
                }
            } catch (Throwable t) {
                t.printStackTrace();
            }
        }
    }
}
