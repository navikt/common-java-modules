package no.nav.dialogarena.mock;

import static no.nav.dialogarena.mock.MockServer.startMockServer;

public class StartJetty {

    public static void main(String[] args) {
        startMockServer("mockservertest", 9123);
    }

}
