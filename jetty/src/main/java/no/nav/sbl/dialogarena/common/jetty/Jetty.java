package no.nav.sbl.dialogarena.common.jetty;

import io.prometheus.client.CollectorRegistry;
import io.prometheus.client.jetty.JettyStatisticsCollector;
import no.nav.brukerdialog.security.jaspic.OidcAuthModule;
import no.nav.brukerdialog.security.oidc.provider.IssoOidcProvider;
import no.nav.common.auth.LoginFilter;
import no.nav.common.auth.LoginProvider;
import org.apache.commons.lang3.ArrayUtils;
import org.eclipse.jetty.annotations.AnnotationConfiguration;
import org.eclipse.jetty.jaas.JAASLoginService;
import org.eclipse.jetty.plus.webapp.EnvConfiguration;
import org.eclipse.jetty.plus.webapp.PlusConfiguration;
import org.eclipse.jetty.security.SecurityHandler;
import org.eclipse.jetty.server.*;
import org.eclipse.jetty.server.handler.StatisticsHandler;
import org.eclipse.jetty.servlet.FilterHolder;
import org.eclipse.jetty.util.component.AbstractLifeCycle;
import org.eclipse.jetty.util.component.LifeCycle;
import org.eclipse.jetty.util.resource.Resource;
import org.eclipse.jetty.webapp.*;
import org.eclipse.jetty.websocket.jsr356.server.deploy.WebSocketServerContainerInitializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.datasource.DriverManagerDataSource;

import javax.naming.NamingException;
import javax.servlet.*;
import javax.sql.DataSource;
import javax.websocket.DeploymentException;
import javax.websocket.server.ServerContainer;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.*;

import static java.lang.System.setProperty;
import static java.util.Collections.*;
import static java.util.Optional.empty;
import static java.util.Optional.of;
import static javax.servlet.DispatcherType.REQUEST;
import static org.apache.commons.io.FilenameUtils.getBaseName;
import static org.apache.commons.lang3.StringUtils.isBlank;
import static org.apache.commons.lang3.StringUtils.isNotBlank;


/**
 * Brukes til å starte opp en embedded Jetty-server, både synkront og asynkront.
 */
public final class Jetty {

    private static final Logger LOG = LoggerFactory.getLogger(Jetty.class);
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
        private Optional<Integer> sslPort = empty();
        private WebAppContext context;
        private File overridewebXmlFile;
        private JAASLoginService loginService;
        private boolean developmentMode;
        private List<Class<?>> websocketEndpoints = new ArrayList<>();
        private Map<String, DataSource> dataSources = new HashMap<>();
        private String extraClasspath;
        private List<Filter> filters = new ArrayList<>();
        private boolean disableAnnotationScanning;
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
            this.sslPort = of(sslPort);
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
                LOG.error("Kunne ikke laste {}", propertyFile, e);
            }
            return this;
        }

        public final JettyBuilder websocketEndpoint(Class<?> endpointClass) {
            this.websocketEndpoints.add(endpointClass);
            return this;
        }

        @Deprecated // Sett heller opp LoginFilter
        public final JettyBuilder withLoginService(JAASLoginService service) {
            this.loginService = service;
            return this;
        }

        //To enable CSRF etc.
        public final JettyBuilder setDeploymentMode() {
            this.developmentMode = false;
            return this;
        }

        public final JettyBuilder addExtraClasspath(String classpath) {
            this.extraClasspath = classpath;
            return this;
        }

        public final JettyBuilder addDatasource(DataSource dataSource, String jndiName) {
            dataSources.put(jndiName, dataSource);
            return this;
        }

        public final JettyBuilder disableStatistics() {
            this.disableStatistics = true;
            return this;
        }

        @Deprecated // Sett heller opp LoginFilter manuelt
        public final JettyBuilder configureForJaspic() {
            return configureForJaspic(emptyList());
        }

        @Deprecated // Sett heller opp LoginFilter manuelt
        public final JettyBuilder configureForJaspic(List<String> ubeskyttet) {
            List<LoginProvider> loginProviders = Collections.singletonList(new OidcAuthModule(Collections.singletonList(new IssoOidcProvider())));
            addFilter(new LoginFilter(loginProviders, ubeskyttet));
            return this;
        }

        public JettyBuilder addFilter(Filter filter) {
            this.filters.add(filter);
            return this;
        }

        public final JettyBuilder addDatasourceByPropertyFile(String propertyFile) throws IOException {
            DriverManagerDataSource dataSource = new DriverManagerDataSource();
            Properties env = readProperties(propertyFile);
            dataSource.setDriverClassName(env.getProperty("driverClassName"));
            dataSource.setUrl(env.getProperty("url"));
            dataSource.setUsername(env.getProperty("username"));
            dataSource.setPassword(env.getProperty("password"));
            String jndiName = env.getProperty("jndiname");

            dataSources.put(jndiName, dataSource);
            return this;
        }

        private static Properties readProperties(String propertyFile) throws IOException {
            Properties env = new Properties();
            env.load(System.class.getResourceAsStream(propertyFile));
            return env;
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
            this.disableAnnotationScanning = true;
            return this;
        }

    }

    private final int port;
    private final Optional<Integer> sslPort;
    private final File overrideWebXmlFile;
    private final String warPath;
    private final JAASLoginService loginService;
    private final String contextPath;
    public final Server server;
    public final WebAppContext context;
    private final Map<String, DataSource> dataSources;
    private final List<Class<?>> websocketEndpoints;
    private final boolean developmentMode;
    private final String extraClasspath;
    private final List<Filter> filters;



    public final Runnable stop = new Runnable() {
        @Override
        public void run() {
            try {
                server.stop();
                server.join();
                LOG.info("JETTY STOPPED");
            } catch (Exception e) {
                throw new RuntimeException(e.getMessage(), e);
            }
        }
    };

    private static final String[] CONFIGURATION_CLASSES = {
            WebInfConfiguration.class.getName(),
            WebXmlConfiguration.class.getName(),
            MetaInfConfiguration.class.getName(),
            FragmentConfiguration.class.getName(),
            JettyWebXmlConfiguration.class.getName(),
            EnvConfiguration.class.getName(),
            PlusConfiguration.class.getName()
    };

    private Jetty(String warPath, JettyBuilder builder) {
        this.warPath = warPath;
        this.overrideWebXmlFile = builder.overridewebXmlFile;
        this.dataSources = builder.dataSources;
        this.websocketEndpoints = builder.websocketEndpoints;
        this.port = builder.port;
        this.sslPort = builder.sslPort;
        this.contextPath = (builder.contextPath.startsWith("/") ? "" : "/") + builder.contextPath;
        this.loginService = builder.loginService;
        this.filters = builder.filters;
        this.extraClasspath = builder.extraClasspath;
        this.context = setupWebapp(builder);
        this.server = setupJetty(new Server(),builder);
        this.developmentMode = builder.developmentMode;
    }

    private WebAppContext setupWebapp(JettyBuilder builder) {
        WebAppContext webAppContext = builder.context;
        if (isNotBlank(contextPath)) {
            webAppContext.setContextPath(contextPath);
        }
        if (isNotBlank(warPath)) {
            webAppContext.setWar(warPath);
        }
        if (isNotBlank(extraClasspath)) {
            webAppContext.setExtraClasspath(extraClasspath);
        }
        if (overrideWebXmlFile != null) {
            webAppContext.setOverrideDescriptor(overrideWebXmlFile.getAbsolutePath());
        }


        if (loginService != null) {
            SecurityHandler securityHandler = webAppContext.getSecurityHandler();
            securityHandler.setLoginService(loginService);
            securityHandler.setRealmName(loginService.getName());
        }

        for (Filter filter : filters) {
            webAppContext.addFilter(new FilterHolder(filter), "/*", EnumSet.of(REQUEST));
        }

        webAppContext.setConfigurationClasses(builder.disableAnnotationScanning ? CONFIGURATION_CLASSES : ArrayUtils.add(CONFIGURATION_CLASSES, AnnotationConfiguration.class.getName()));
        Map<String, String> initParams = webAppContext.getInitParams();
        if(developmentMode) {
            initParams.put("org.eclipse.jetty.servlet.Default.useFileMappedBuffer", "false"); // ikke hold filer i minne slik at de låses i windoze
        }
        initParams.put("org.eclipse.jetty.servlet.Default.etags", "true");
        initParams.put("org.eclipse.jetty.servlet.SessionIdPathParameterName", "none"); // Forhindre url rewriting av sessionid
        webAppContext.setAttribute(WebInfConfiguration.CONTAINER_JAR_PATTERN, CLASSPATH_PATTERN);
        addDatasource(webAppContext);
        return webAppContext;
    }

    private void addDatasource(WebAppContext webAppContext) {
        if (!dataSources.isEmpty()) {
            for (Map.Entry<String, DataSource> entrySet : dataSources.entrySet()) {
                try {
                    new org.eclipse.jetty.plus.jndi.Resource(webAppContext, entrySet.getKey(), entrySet.getValue());
                } catch (NamingException e) {
                    throw new RuntimeException("Kunne ikke legge til datasource " + e, e);
                }
            }
        }
    }

    private void addWebsocketEndpoints(WebAppContext webAppContext) {
        if (!websocketEndpoints.isEmpty()) {
            try {
                ServerContainer wscontainer = WebSocketServerContainerInitializer.configureContext(webAppContext);
                websocketEndpoints.forEach(endpoint -> {
                    try {
                        wscontainer.addEndpoint(endpoint);
                    } catch (DeploymentException e) {
                        throw new RuntimeException("Kunne ikke adde websocket endpoint " + e, e);
                    }
                });

            } catch (ServletException e) {
                throw new RuntimeException("Kunne ikke adde websocket endpoint " + e, e);
            }
        }
    }

    private Server setupJetty(final Server jetty, JettyBuilder jettyBuilder) {
        if(developmentMode){
            Resource.setDefaultUseCaches(false);
        }

        HttpConfiguration configuration = new HttpConfiguration();
        // Increase default buffer sizes to prevent failures due to large responses
        configuration.setResponseHeaderSize(16384);
        configuration.setOutputBufferSize(32768);

        // Add support for X-Forwarded headers
        configuration.addCustomizer(new ForwardedRequestCustomizer());

        ServerConnector httpConnector = new ServerConnector(jetty, new HttpConnectionFactory(configuration));
        httpConnector.setSoLingerTime(-1);
        httpConnector.setPort(port);

        Optional<ServerConnector> sslConnector = sslPort.map(new CreateSslConnector(jetty, configuration));
        List<Connector> liste = new ArrayList<>();
        liste.add(httpConnector);

        sslConnector.ifPresent(liste::add);

        Connector[] connectors = liste.toArray(new Connector[liste.size()]);
        jetty.setConnectors(connectors);
        context.setServer(jetty);
        addWebsocketEndpoints(context);

        jetty.setHandler(context);

        if(!jettyBuilder.disableStatistics){
            addStatisticsHandler(jetty);
        }

        return jetty;
    }

    public static void addStatisticsHandler(Server server) {
        StatisticsHandler statisticsHandler = new StatisticsHandler();
        statisticsHandler.setHandler(server.getHandler());
        server.setHandler(statisticsHandler);

        CollectorRegistry collectorRegistry = CollectorRegistry.defaultRegistry;
        JettyStatisticsCollector jettyStatisticsCollector = new JettyStatisticsCollector(statisticsHandler);
        server.addLifeCycleListener(new AbstractLifeCycle.AbstractLifeCycleListener() {

            @Override
            public void lifeCycleStarting(LifeCycle event) {
                jettyStatisticsCollector.register(collectorRegistry);
            }

            @Override
            public void lifeCycleStopped(LifeCycle event) {
                collectorRegistry.unregister(jettyStatisticsCollector);
            }
        });
    }

    public Jetty start() {
        return startAnd(() -> {

        });
    }

    public Jetty startAnd(Runnable doWhenStarted) {
        try {
            disableSecureCookies();

            setRunMode();
            server.start();
            LOG.info("STARTED JETTY");
            LOG.info(" * WAR: " + warPath);
            LOG.info(" * Context path: " + contextPath);
            LOG.info(" * Http port: " + port);
            sslPort.ifPresent(integer -> LOG.info(" * Https port: " + integer));
            for (URL url : getBaseUrls()) {
                LOG.info(" * " + url);
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
        sslPort.ifPresent(integer -> urls.add(new ToUrl("https", contextPath).apply(integer)));
        return urls;
    }

    private static File getWebappSource() {
        return getFileSource("src/main/webapp");
    }

    private static File getTestResourceSource() {
        return getFileSource("src/test/resources");
    }

    private static File getFileSource(String filesource) {
        File baseDir = null;
        try {
            File classesDir = new File(Jetty.class.getResource("/").toURI());
            baseDir = new File(classesDir, "../../").getCanonicalFile();
        } catch (URISyntaxException | IOException e) {
            LOG.error("Krasjet under opprettelsen av {}", filesource, e);
        }
        return new File(baseDir, filesource);
    }

    private static void loadPropertiesFile(String propertyFile) throws IOException {
        Properties properties = new Properties();
        InputStream inputStream = System.class.getResourceAsStream(propertyFile);
        properties.load(inputStream);

        for (Map.Entry<Object, Object> entry : properties.entrySet()) {
            setProperty((String) entry.getKey(), (String) entry.getValue());
        }
    }

    private void disableSecureCookies() {
        if (developmentMode) {
            LOG.warn("Forcing session cookies to be insecure. DO NOT USE IN PRODUCTION!");
            context.addEventListener(new ServletContextListener() {
                @Override
                public void contextInitialized(ServletContextEvent sce) {
                    sce.getServletContext().getSessionCookieConfig().setSecure(false);
                }

                @Override
                public void contextDestroyed(ServletContextEvent sce) {
                }
            });
        }
    }

    private void setRunMode() {
        if (developmentMode) {
            LOG.warn("Setting development mode. DO NOT USE IN PRODUCTION!");
            LOG.warn("Override development mode by setting System property wicket.configuration to deployment");
            setProperty("wicket.configuration", "development");
        } else {
            setProperty("wicket.configuration", "deployment");
        }
    }
}
