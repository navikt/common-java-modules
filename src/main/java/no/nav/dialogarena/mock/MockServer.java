package no.nav.dialogarena.mock;

import no.nav.sbl.dialogarena.common.jetty.Jetty;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MockServer {

    public static Jetty startMockServer(String contextName, int jettyPort) {
        final Jetty jetty = Jetty.usingWar()
                .at(contextName)
                .port(jettyPort)
                .overrideWebXml()
                .buildJetty();
        jetty.server.setHandler(new MockHandler(contextName));
        jetty.start();
        return jetty;
    }

}
