package no.nav.common.cxf.jetty;

import static jakarta.servlet.DispatcherType.REQUEST;
import static java.lang.System.setProperty;
import static org.apache.commons.io.FilenameUtils.getBaseName;
import static org.apache.commons.lang3.StringUtils.isBlank;
import static org.apache.commons.lang3.StringUtils.isNotBlank;

import jakarta.servlet.Filter;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import javax.sql.DataSource;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.jetty.ee11.servlet.FilterHolder;
import org.eclipse.jetty.ee11.webapp.WebAppContext;
import org.eclipse.jetty.server.Connector;
import org.eclipse.jetty.server.ForwardedRequestCustomizer;
import org.eclipse.jetty.server.HttpConfiguration;
import org.eclipse.jetty.server.HttpConnectionFactory;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.ServerConnector;
import org.eclipse.jetty.server.handler.StatisticsHandler;

/**
 * Starter en embedded Jetty-server for CXF-testene.
 */
@SuppressWarnings("unused")
@Slf4j
public final class Jetty {

    static final String CLASSPATH_PATTERN = ".*(\\.jar|/(test-)?classes/?)";

    public static JettyBuilder usingWar(File file) {
        return new JettyBuilder().war(file);
    }

    public static JettyBuilder usingWar() {
        return new JettyBuilder().war(getWebappSource());
    }

    /**
     * Builder for å konfigurere opp en Jetty-instans.
     */
    public static class JettyBuilder {
        private File war;
        private String contextPath;
        private int port = 35000;
        private Integer sslPort;
        private WebAppContext context;
        private File overridewebXmlFile;
        private String extraClasspath;
        private final List<Filter> filters = new ArrayList<>();
        private final List<JettyCustomizer> customizers = new ArrayList<>();
        private boolean disableStatistics;

        public final JettyBuilder war(File warPath) {
            this.war = warPath;
            return this;
        }

        public final JettyBuilder at(String ctxPath) {
            this.contextPath = ctxPath;
            return this;
        }

        public final JettyBuilder port(int jettyPort) {
            this.port = jettyPort;
            return this;
        }

        public final JettyBuilder sslPort(int sslPort) {
            this.sslPort = sslPort;
            return this;
        }

        public final JettyBuilder overrideWebXml(File overrideWebXmlFile) {
            this.overridewebXmlFile = overrideWebXmlFile;
            return this;
        }

        public final JettyBuilder overrideWebXml() {
            this.overridewebXmlFile = new File(getTestResourceSource(), "override-web.xml");
            return this;
        }

        public final JettyBuilder loadProperties(String propertyFile) {
            try {
                loadPropertiesFile(propertyFile);
            } catch (IOException e) {
                log.error("Kunne ikke laste {}", propertyFile, e);
            }
            return this;
        }

        @SuppressWarnings("java:S1172") // Beholdt for kompatibilitet; registreringen hadde ingen effekt.
        public final JettyBuilder websocketEndpoint(Class<?> endpointClass) {
            return this;
        }

        public final JettyBuilder setDeploymentMode() {
            return this;
        }

        public final JettyBuilder addExtraClasspath(String classpath) {
            this.extraClasspath = classpath;
            return this;
        }

        @SuppressWarnings("java:S1172") // Beholdt for kompatibilitet; datakildene ble ikke brukt.
        public final JettyBuilder addDatasource(DataSource dataSource, String jndiName) {
            return this;
        }

        public final JettyBuilder disableStatistics() {
            this.disableStatistics = true;
            return this;
        }

        public JettyBuilder addFilter(Filter filter) {
            this.filters.add(filter);
            return this;
        }

        public JettyBuilder addCustomizer(JettyCustomizer jettyCustomizer) {
            this.customizers.add(jettyCustomizer);
            return this;
        }

        public final Jetty buildJetty() {
            try {
                if (context == null) {
                    context = new WebAppContext();
                    context.setThrowUnavailableOnStartupException(true);
                }
                String warPath = getWarPath();
                if (isBlank(contextPath)) {
                    contextPath = getBaseName(warPath);
                }
                return new Jetty(warPath, this);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }

        private String getWarPath() throws IOException {
            if (war != null) {
                return war.getCanonicalPath();
            } else {
                return context.getWar();
            }
        }

        public JettyBuilder disableAnnotationScanning() {
            return this;
        }
    }

    private final int port;
    private final Integer sslPort;
    private final File overrideWebXmlFile;
    private final String warPath;
    private final String contextPath;
    public final Server server;
    public final WebAppContext context;

    public final Runnable stop = new Runnable() {
        @Override
        public void run() {
            try {
                server.stop();
                server.join();
                log.info("JETTY STOPPED");
            } catch (Exception e) {
                throw new RuntimeException(e.getMessage(), e);
            }
        }
    };

    private Jetty(String warPath, JettyBuilder builder) {
        this.warPath = warPath;
        this.overrideWebXmlFile = builder.overridewebXmlFile;
        this.port = builder.port;
        this.sslPort = builder.sslPort;
        this.contextPath = (builder.contextPath.startsWith("/") ? "" : "/") + builder.contextPath;
        this.context = setupWebapp(builder);
        this.server = setupJetty(new Server(), builder);
    }

    private WebAppContext setupWebapp(JettyBuilder builder) {
        WebAppContext webAppContext = builder.context;
        if (isNotBlank(contextPath)) {
            webAppContext.setContextPath(contextPath);
        }
        if (isNotBlank(warPath)) {
            webAppContext.setWar(warPath);
        }
        if (isNotBlank(builder.extraClasspath)) {
            webAppContext.setExtraClasspath(builder.extraClasspath);
        }
        if (overrideWebXmlFile != null) {
            webAppContext.setOverrideDescriptor(overrideWebXmlFile.getAbsolutePath());
        }

        for (Filter filter : builder.filters) {
            webAppContext.addFilter(new FilterHolder(filter), "/*", EnumSet.of(REQUEST));
        }

        Map<String, String> initParams = webAppContext.getInitParams();
        initParams.put("org.eclipse.jetty.servlet.Default.etags", "true");
        initParams.put(
                "org.eclipse.jetty.servlet.SessionIdPathParameterName", "none"); // Forhindre url rewriting av sessionid
        webAppContext.setAttribute("org.eclipse.jetty.server.webapp.ContainerIncludeJarPattern", CLASSPATH_PATTERN);
        return webAppContext;
    }

    private Server setupJetty(final Server jetty, JettyBuilder builder) {
        HttpConfiguration configuration = new HttpConfiguration();
        // Increase default buffer sizes to prevent failures due to large requests or responses
        // This increases memory usage but seems to be necessary to handle current heavy usage of cookies (!)
        configuration.setRequestHeaderSize(16384);
        configuration.setResponseHeaderSize(16384);
        configuration.setOutputBufferSize(32768);

        // Add support for X-Forwarded headers
        configuration.addCustomizer(new ForwardedRequestCustomizer());

        builder.customizers.forEach(customizer -> customizer.customize(configuration));

        ServerConnector httpConnector = new ServerConnector(jetty, new HttpConnectionFactory(configuration));
        httpConnector.setPort(port);

        List<Connector> liste = new ArrayList<>();
        liste.add(httpConnector);

        if (sslPort != null) {
            liste.add(new CreateSslConnector(jetty, configuration).apply(sslPort));
        }

        Connector[] connectors = liste.toArray(new Connector[0]);
        jetty.setConnectors(connectors);
        context.setServer(jetty);

        builder.customizers.forEach(customizer -> customizer.customize(context));
        jetty.setHandler(context);

        if (!builder.disableStatistics) {
            addStatisticsHandler(jetty);
        }
        builder.customizers.forEach(customizer -> customizer.customize(jetty));

        return jetty;
    }

    public static void addStatisticsHandler(Server server) {
        StatisticsHandler statisticsHandler = new StatisticsHandler();
        statisticsHandler.setHandler(server.getHandler());
        server.setHandler(statisticsHandler);
    }

    @SuppressWarnings("UnusedReturnValue")
    public Jetty start() {
        return startAnd(() -> {});
    }

    public Jetty startAnd(Runnable doWhenStarted) {
        try {
            setProperty("wicket.configuration", "deployment");
            server.start();
            log.info("STARTED JETTY");
            log.info(" * WAR: {}", warPath);
            log.info(" * Context path: {}", contextPath);
            log.info(" * Http port: {}", port);
            if (sslPort != null) {
                log.info(" * Https port: {}", sslPort);
            }
            for (URL url : getBaseUrls()) {
                log.info(" * {}", url);
            }
            doWhenStarted.run();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        return this;
    }

    public List<URL> getBaseUrls() {
        List<URL> urls = new ArrayList<>();
        urls.add(new ToUrl("http", contextPath).apply(port));
        if (sslPort != null) {
            urls.add(new ToUrl("https", contextPath).apply(sslPort));
        }
        return urls;
    }

    private static File getWebappSource() {
        return getFileSource("src/main/webapp");
    }

    private static File getTestResourceSource() {
        return getFileSource("src/test/resources");
    }

    static File getFileSource(String filesource) {
        try {
            File classesDir = new File(Jetty.class.getResource("/").toURI());
            for (File baseDir = classesDir; baseDir != null; baseDir = baseDir.getParentFile()) {
                File source = new File(baseDir, filesource);
                if (source.isDirectory()) {
                    return source;
                }
            }
            throw new IllegalStateException("Fant ikke " + filesource + " fra " + classesDir);
        } catch (URISyntaxException e) {
            throw new IllegalStateException("Kunne ikke finne " + filesource, e);
        }
    }

    private static void loadPropertiesFile(String propertyFile) throws IOException {
        Properties properties = new Properties();
        try (InputStream inputStream = System.class.getResourceAsStream(propertyFile)) {
            properties.load(inputStream);
        }

        for (Map.Entry<Object, Object> entry : properties.entrySet()) {
            setProperty((String) entry.getKey(), (String) entry.getValue());
        }
    }
}
