package no.nav.fo.apiapp;

import org.apache.commons.io.IOUtils;

import java.io.IOException;
import java.io.InputStream;
import java.net.ServerSocket;
import java.net.Socket;


public class StartMetricsSocket {

    // emulerer sensu
    public static void main(String[] args) throws IOException {
        ServerSocket serverSocket = new ServerSocket(3030);
        while (true) {
            IOUtils.copy(serverSocket.accept().getInputStream(), System.out);
        }
    }

}
