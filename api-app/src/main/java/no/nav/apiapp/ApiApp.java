package no.nav.apiapp;

import lombok.SneakyThrows;
import no.nav.apiapp.ApiApplication.NaisApiApplication;
import no.nav.apiapp.config.Konfigurator;
import no.nav.metrics.Event;
import no.nav.metrics.MetricsFactory;
import no.nav.sbl.dialogarena.common.jetty.Jetty;
import org.eclipse.jetty.webapp.WebAppContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.ServletContextListener;
import java.io.File;
import java.io.IOException;

import static no.nav.apiapp.ApiAppServletContextListener.SPRING_CONTEKST_KLASSE_PARAMETER_NAME;
import static no.nav.sbl.dialogarena.common.jetty.Jetty.usingWar;
import static no.nav.sbl.util.EnvironmentUtils.Type.PUBLIC;
import static no.nav.sbl.util.EnvironmentUtils.Type.SECRET;
import static no.nav.sbl.util.EnvironmentUtils.getOptionalProperty;
import static no.nav.sbl.util.EnvironmentUtils.setProperty;

public class ApiApp {

    public static final String TRUSTSTORE = "javax.net.ssl.trustStore";
    public static final String TRUSTSTOREPASSWORD = "javax.net.ssl.trustStorePassword";
    public static final String NAV_TRUSTSTORE_PATH = "NAV_TRUSTSTORE_PATH";
    public static final String NAV_TRUSTSTORE_PASSWORD = "NAV_TRUSTSTORE_PASSWORD";

    private static final Logger LOGGER = LoggerFactory.getLogger(ApiApp.class);

    static final int DEFAULT_HTTP_PORT = 8080;

    @SneakyThrows
    public static void startApp(Class<? extends NaisApiApplication> apiAppClass, String[] args) {
        long start = System.currentTimeMillis();
        setupTrustStore();
        NaisApiApplication apiApplication = apiAppClass.newInstance();
        Jetty jetty = setupJetty(apiApplication, args);
        reportStartupTime(start);
        jetty.server.join();
    }

    private static Jetty setupJetty(NaisApiApplication apiApplication, String[] args) throws IOException, InstantiationException, IllegalAccessException {
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

        String contextPath = apiApplication.brukContextPath() ? "/" + apiApplication.getApplicationName() : "/";

        Jetty.JettyBuilder jettyBuilder = usingWar(file)
                .at(contextPath)
                .port(httpPort)
                .disableAnnotationScanning();
        Konfigurator konfigurator = new Konfigurator(jettyBuilder, apiApplication);
        apiApplication.configure(konfigurator);
        Jetty jetty = konfigurator.buildJetty();

        WebAppContext webAppContext = jetty.context;
        webAppContext.setInitParameter(SPRING_CONTEKST_KLASSE_PARAMETER_NAME, apiApplication.getClass().getName());
        ServletContextListener listener = new ApiAppServletContextListener(konfigurator);
        webAppContext.addEventListener(listener);
        webAppContext.setClassLoader(Thread.currentThread().getContextClassLoader());

        try {
            jetty.start();
        } catch (Throwable t) {
            LOGGER.error(t.getMessage(), t);
            System.exit(1);
        }

        Runtime.getRuntime().addShutdownHook(new ShutdownHook(jetty));
        return jetty;
    }

    private static void setupTrustStore() {
        if (getOptionalProperty(TRUSTSTORE).isPresent()) {
            return;
        }
        getOptionalProperty(NAV_TRUSTSTORE_PATH).ifPresent(path -> setProperty(TRUSTSTORE, path, PUBLIC));
        getOptionalProperty(NAV_TRUSTSTORE_PASSWORD).ifPresent(passwd -> setProperty(TRUSTSTOREPASSWORD, passwd, SECRET));
    }

    private static void reportStartupTime(long start) {
        long startupTime = System.currentTimeMillis() - start;
        LOGGER.info("oppstartstid: {} ms", startupTime);
        Event event = MetricsFactory.createEvent("startup");
        event.addFieldToReport("time", startupTime);
        event.report();
    }

    private static int httpPort(String[] args) {
        if (args.length > 0) {
            return Integer.parseInt(args[0]);
        } else {
            return DEFAULT_HTTP_PORT;
        }
    }

}
