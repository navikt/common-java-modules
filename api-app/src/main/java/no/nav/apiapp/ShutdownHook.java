package no.nav.apiapp;

import no.nav.sbl.dialogarena.common.jetty.Jetty;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ShutdownHook extends Thread {

    private static final Logger LOGGER = LoggerFactory.getLogger(ShutdownHook.class);

    public ShutdownHook(Jetty jetty) {
        super(new Hook(jetty));
    }

    private static class Hook implements Runnable {
        private final Jetty jetty;

        public Hook(Jetty jetty) {
            this.jetty = jetty;
        }

        @Override
        public void run() {
            try {
                LOGGER.info("shutdown initialized, allowing incoming requests for 15 seconds before continuing");
                // https://doc.nais.io/nais-application#handles-termination-gracefully
                Thread.sleep(15000L);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }

            LOGGER.info("shutting down server");
            jetty.stop.run();
            LOGGER.info("shutdown ok");
        }
    }
}
