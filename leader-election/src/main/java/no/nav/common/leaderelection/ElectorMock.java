package no.nav.common.leaderelection;

import com.github.tomakehurst.wiremock.WireMockServer;
import lombok.SneakyThrows;

import java.net.InetAddress;

import static com.github.tomakehurst.wiremock.client.WireMock.*;

public class ElectorMock {

    public static WireMockServer server() {
        WireMockServer electorServer = new WireMockServer(4040);
        electorServer.stubFor(get(
                urlEqualTo("/"))
                .willReturn(
                        aResponse()
                                .withHeader("Content-Type", "text/plain; charset=utf-8")
                                .withBody(String.format("{\"name:\" : \"%s\"}", getHostName()))
                ));
        return electorServer;
    }

    public static void start() {
        server().start();
    }

    @SneakyThrows
    private static String getHostName() {
        return InetAddress.getLocalHost().getHostName();
    }

}
