package no.nav.apiapp;

import lombok.SneakyThrows;
import no.nav.apiapp.config.Konfigurator;
import no.nav.apiapp.feil.FeilMapper;
import no.nav.apiapp.util.UrlUtils;
import no.nav.apiapp.util.WarFolderFinderUtil;
import no.nav.metrics.Event;
import no.nav.metrics.MetricsFactory;
import no.nav.sbl.dialogarena.common.jetty.Jetty;
import no.nav.sbl.dialogarena.common.jetty.JettyCustomizer;
import no.nav.util.sbl.StringUtils;
import org.eclipse.jetty.server.HttpConfiguration;
import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.handler.AbstractHandler;
import org.eclipse.jetty.server.handler.ErrorHandler;
import org.eclipse.jetty.server.handler.HandlerCollection;
import org.eclipse.jetty.webapp.WebAppContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.net.URLClassLoader;

import static no.nav.sbl.dialogarena.common.jetty.Jetty.usingWar;
import static no.nav.util.sbl.EnvironmentUtils.Type.PUBLIC;
import static no.nav.util.sbl.EnvironmentUtils.Type.SECRET;
import static no.nav.util.sbl.EnvironmentUtils.getOptionalProperty;
import static no.nav.util.sbl.EnvironmentUtils.setProperty;

public class ApiApp {

    public static final String TRUSTSTORE = "javax.net.ssl.trustStore";
    public static final String TRUSTSTOREPASSWORD = "javax.net.ssl.trustStorePassword";
    public static final String NAV_TRUSTSTORE_PATH = "NAV_TRUSTSTORE_PATH";
    public static final String NAV_TRUSTSTORE_PASSWORD = "NAV_TRUSTSTORE_PASSWORD";

    private static final Logger LOGGER = LoggerFactory.getLogger(ApiApp.class);

    static final int DEFAULT_HTTP_PORT = 8080;

    private final Jetty jetty;

    public ApiApp(Jetty jetty) {
        this.jetty = jetty;
    }

    public Jetty getJetty() {
        return jetty;
    }

    @SneakyThrows
    @Deprecated // use runApp
    public static void startApp(Class<? extends ApiApplication> apiAppClass, String[] args) {
        runApp(apiAppClass,args);
    }

    @SneakyThrows
    public static void runApp(Class<? extends ApiApplication> apiAppClass, String[] args) {
        ApiApp apiApp = startApiApp(apiAppClass, args);
        Jetty jetty = apiApp.jetty;
        jetty.server.join();
    }

    @SneakyThrows
    public static ApiApp startApiApp(Class<? extends ApiApplication> apiAppClass, String[] args) {
        long start = System.currentTimeMillis();
        setupTrustStore();
        ApiApplication apiApplication = apiAppClass.newInstance();
        Jetty jetty = setupJetty(apiApplication, args);
        reportStartupTime(start);
        return new ApiApp(jetty);
    }

    private static Jetty setupJetty(ApiApplication apiApplication, String[] args) throws IOException {
        int httpPort = httpPort(args);

        File file = WarFolderFinderUtil.findPath(apiApplication.getClass());
        LOGGER.info("starter med war på: {}", file.getCanonicalPath());

        String contextPath = StringUtils.of(apiApplication.getContextPath()).map(UrlUtils::startMedSlash).orElse("/");

        Jetty.JettyBuilder jettyBuilder = usingWar(file)
                .at(contextPath)
                .port(httpPort)
                .disableAnnotationScanning()
                .disableStatistics() // statistics should be added last
                .addCustomizer(new JettyCustomizer() {
                    @Override
                    public void customize(HttpConfiguration httpConfiguration) {
                        httpConfiguration.setSendServerVersion(false);
                    }
                });

        if (args.length > 1) {
            jettyBuilder.sslPort(Integer.parseInt(args[1]));
        }

        Konfigurator konfigurator = new Konfigurator(jettyBuilder, apiApplication);
        apiApplication.configure(konfigurator);
        Jetty jetty = konfigurator.buildJetty();

        WebAppContext webAppContext = jetty.context;
        webAppContext.addEventListener(new ApiAppServletContextListener(konfigurator, apiApplication));

        Server server = jetty.server;
        if (contextPath.length() > 1) {
            HandlerCollection handlerCollection = new HandlerCollection();
            handlerCollection.addHandler(server.getHandler());
            handlerCollection.addHandler(new RootToContextRedirectHandler(contextPath));
            server.setHandler(handlerCollection);
        }

        Jetty.addStatisticsHandler(server);
        // When we embed jetty in this way, some classes might be loaded by the default WebAppClassLoader and some by the system class loader.
        // These classes will be incompatible with each other. Also, Jetty does not consult the classloader of the webapp when resolving resources
        // such as the swagger-ui. We mitigate both these problems by installing an empty classloader that will always defer to the system classloader
        webAppContext.setClassLoader(URLClassLoader.newInstance(new URL[0]));

        ErrorHandler errorHandler = new ErrorHandler();
        errorHandler.setShowStacks(FeilMapper.visDetaljer());
        jetty.server.setErrorHandler(errorHandler);
        jetty.context.setErrorHandler(errorHandler);

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

    private static class RootToContextRedirectHandler extends AbstractHandler {
        private final String contextPath;

        private RootToContextRedirectHandler(String contextPath) {
            this.contextPath = contextPath;
        }

        @Override
        public void handle(String requestPath, Request request, HttpServletRequest httpServletRequest, HttpServletResponse httpServletResponse) throws IOException, ServletException {
            if ("/".equals(requestPath)) {
                httpServletResponse.sendRedirect(contextPath);
                request.setHandled(true);
            }
        }
    }
}
