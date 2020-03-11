package no.nav.fo.apiapp.rest;

import no.nav.apiapp.ApiApp;
import no.nav.apiapp.ApiApplication;
import no.nav.fo.apiapp.JettyTestConfig;
import no.nav.sbl.dialogarena.common.jetty.Jetty;
import no.nav.testconfig.ApiAppTest;
import org.eclipse.jetty.server.ServerConnector;

import java.io.IOException;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.UnknownHostException;

import static no.nav.testconfig.ApiAppTest.DEFAULT_ENVIRONMENT;
public class JettyTestUtils {

    public static final String APPLICATION_NAME = "api-app";

    public static void setupContext() {
        ApiAppTest.setupTestContext(ApiAppTest.Config.builder()
                .applicationName(APPLICATION_NAME)
                .environment(DEFAULT_ENVIRONMENT)
                .build()
        );
    }

    public static void setupContext(JettyTestConfig testConfig) {
        ApiAppTest.setupTestContext(ApiAppTest.Config.builder()
                .applicationName(APPLICATION_NAME)
                .environment(DEFAULT_ENVIRONMENT)
                .allowClientStorage(testConfig.isAllowClientStorage())
                .disablePragmaHeader(testConfig.isDisablePragmaHeader())
                .build()
        );
    }

    public static Jetty nyJettyForTest(Class<? extends ApiApplication> apiAppClass) {
        ApiApp apiApp = ApiApp.startApiApp(apiAppClass, new String[]{Integer.toString(tilfeldigPort()), Integer.toString(tilfeldigPort())});
        return apiApp.getJetty();
    }

    public static int tilfeldigPort() {
        try (ServerSocket serverSocket = new ServerSocket(0)) {
            return serverSocket.getLocalPort();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static String getHostName() {
        try {
            return InetAddress.getLocalHost().getCanonicalHostName();
        } catch (UnknownHostException e) {
            throw new RuntimeException(e);
        }
    }

    public static int getSslPort(Jetty jetty) {
        return ((ServerConnector) jetty.server.getConnectors()[1]).getPort();
    }
}
