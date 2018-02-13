package com.github.wrm.pact;

import java.io.IOException;
import java.net.ServerSocket;

public class OpenPortProvider {
    public static int getOpenPort() {
        try (ServerSocket socket = new ServerSocket(0)) {
            socket.setReuseAddress(true);
            return socket.getLocalPort();
        }
        catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}