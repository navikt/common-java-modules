package no.nav.apiapp;

import lombok.SneakyThrows;
import no.nav.metrics.MetricsFactory;
import no.nav.sbl.dialogarena.common.jetty.Jetty;
import org.eclipse.jetty.webapp.WebAppContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.ServletContextListener;
import java.io.File;

import static no.nav.apiapp.ApiAppServletContextListener.SPRING_CONTEKST_KLASSE_PARAMETER_NAME;

public class ApiApp {

    private static final Logger LOGGER = LoggerFactory.getLogger(ApiApp.class);

    static final int DEFAULT_HTTP_PORT = 8080;

    @SneakyThrows
    public static void startApp(Class<? extends ApiApplication> apiAppClass, String[] args) {
        long start = System.currentTimeMillis();
        int httpPort = httpPort(args);

        // TODO disable logging til fil!
        // TODO gå gjennom common-jetty og gjøre dette mer prodklart!

        File runtimePath = new File("/app");
        File sourcePath = new File(".", "src/main");
        if (sourcePath.exists()) {
            new File(sourcePath, "webapp").mkdir();
        }
        File devPath = new File(".", "src/main/webapp");
        File file = devPath.exists() ? devPath : runtimePath;
        LOGGER.info("starter med war på: {}", file.getCanonicalPath());

        Jetty jetty = Jetty.usingWar(file)
                .at("/" + apiAppClass.newInstance().getApplicationName())
                .port(httpPort)
                .disableAnnotationScanning()
                .buildJetty();

        WebAppContext webAppContext = jetty.context;
        webAppContext.setInitParameter(SPRING_CONTEKST_KLASSE_PARAMETER_NAME, apiAppClass.getName());
        ServletContextListener listener = new ApiAppServletContextListener();
        webAppContext.addEventListener(listener);
        webAppContext.setClassLoader(Thread.currentThread().getContextClassLoader());

        try {
            jetty.start();
        } catch (Throwable t) {
            LOGGER.error(t.getMessage(), t);
            System.exit(1);
        }

        Runtime.getRuntime().addShutdownHook(new ShutdownHook(jetty));
        System.setProperty(MetricsFactory.DISABLE_METRICS_REPORT_KEY,Boolean.TRUE.toString());
        LOGGER.info("oppstartstid: {} ms", System.currentTimeMillis() - start);
        jetty.server.join();
    }

    private static int httpPort(String[] args) {
        if (args.length > 0) {
            return Integer.parseInt(args[0]);
        } else {
            return DEFAULT_HTTP_PORT;
        }
    }
}
