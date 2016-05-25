package no.nav.sbl.dialogarena.common.jetty;

import com.ibm.mq.jms.JMSC;
import com.ibm.mq.jms.MQQueueConnectionFactory;
import org.eclipse.jetty.jaas.JAASLoginService;
import org.eclipse.jetty.plus.webapp.EnvConfiguration;
import org.eclipse.jetty.plus.webapp.PlusConfiguration;
import org.eclipse.jetty.security.SecurityHandler;
import org.eclipse.jetty.server.*;
import org.eclipse.jetty.util.resource.Resource;
import org.eclipse.jetty.webapp.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.jms.connection.UserCredentialsConnectionFactoryAdapter;

import javax.jms.*;
import javax.jms.Queue;
import javax.naming.NamingException;
import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;
import javax.sql.DataSource;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.*;

import static java.lang.System.setProperty;
import static java.util.Arrays.asList;
import static java.util.Optional.empty;
import static java.util.Optional.of;
import static no.nav.modig.testcertificates.TestCertificates.setupKeyAndTrustStore;
import static org.apache.commons.io.FilenameUtils.getBaseName;
import static org.apache.commons.lang3.StringUtils.isBlank;
import static org.apache.commons.lang3.StringUtils.isNotBlank;


/**
 * Brukes til å starte opp en embedded Jetty-server, både synkront og asynkront.
 */
public final class Jetty {

    private static final Logger LOG = LoggerFactory.getLogger(Jetty.class);

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
        private Boolean developmentMode = true;
        private Map<String, DataSource> dataSources = new HashMap<>();
        private Map<String, UserCredentialsConnectionFactoryAdapter> connectionSources = new HashMap<>();
        private Map<String, Queue> queueSources = new HashMap<>();
        private String extraClasspath;

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

        public final JettyBuilder addMQConnection(String propertyFile) throws JMSException, IOException {
            MQQueueConnectionFactory mqQueueConnectionFactory = new MQQueueConnectionFactory();
            Properties env = readProperties(propertyFile);

            mqQueueConnectionFactory.setHostName(env.getProperty("hostname"));
            mqQueueConnectionFactory.setPort(Integer.parseInt(env.getProperty("port")));
            mqQueueConnectionFactory.setChannel(env.getProperty("channel"));
            mqQueueConnectionFactory.setTransportType(JMSC.MQJMS_TP_CLIENT_MQ_TCPIP);
            mqQueueConnectionFactory.setQueueManager(env.getProperty("queuemanager"));

            UserCredentialsConnectionFactoryAdapter usrcCredentialsFc = new UserCredentialsConnectionFactoryAdapter();
            usrcCredentialsFc.setUsername(env.getProperty("username"));
            usrcCredentialsFc.setPassword(env.getProperty("password"));
            usrcCredentialsFc.setTargetConnectionFactory(mqQueueConnectionFactory);

            QueueConnection connection = usrcCredentialsFc.createQueueConnection();
            QueueSession session = connection.createQueueSession(false, Session.AUTO_ACKNOWLEDGE);

            connectionSources.put(env.getProperty("jndi-connection-name"), usrcCredentialsFc);
            queueSources.put(env.getProperty("jndi-queue-name"), session.createQueue(env.getProperty("queuename")));
            return this;
        }

        public final Jetty buildJetty() {
            setProperty("u", "true");
            setProperty("disable.ssl.cn.check", "true");
            setupKeyAndTrustStore();

            try {
                if (context == null) {
                    context = new WebAppContext();
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
    }

    private final int port;
    private final Optional<Integer> sslPort;
    private final File overrideWebXmlFile;
    private final String warPath;
    private final String contextPath;
    private final JAASLoginService loginService;
    public final Server server;
    public final WebAppContext context;
    private final Map<String, DataSource> dataSources;
    private final Map<String, UserCredentialsConnectionFactoryAdapter> connections;
    private final Map<String, Queue> queues;
    private final Boolean developmentMode;
    private final String extraClasspath;

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
        this.connections = builder.connectionSources;
        this.queues = builder.queueSources;
        this.port = builder.port;
        this.sslPort = builder.sslPort;
        this.contextPath = (builder.contextPath.startsWith("/") ? "" : "/") + builder.contextPath;
        this.loginService = builder.loginService;
        this.extraClasspath = builder.extraClasspath;
        this.context = setupWebapp(builder.context);
        this.server = setupJetty(new Server());
        this.developmentMode = builder.developmentMode;
    }

    private WebAppContext setupWebapp(final WebAppContext webAppContext) {
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

        webAppContext.setConfigurationClasses(CONFIGURATION_CLASSES);
        Map<String, String> initParams = webAppContext.getInitParams();
        initParams.put("org.eclipse.jetty.servlet.Default.useFileMappedBuffer", "false"); // ikke hold filer i minne slik at de låses i windoze
        initParams.put("org.eclipse.jetty.servlet.SessionIdPathParameterName", "none"); // Forhindre url rewriting av sessionid
        webAppContext.setAttribute(WebInfConfiguration.CONTAINER_JAR_PATTERN, ".*");


        addDatasource(webAppContext);
        addConnection(webAppContext);
        addQueue(webAppContext);

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

    private void addConnection(WebAppContext webAppContext) {
        if (!connections.isEmpty()) {
            for (Map.Entry<String, UserCredentialsConnectionFactoryAdapter> entrySet : connections.entrySet()) {
                try {
                    new org.eclipse.jetty.plus.jndi.Resource(webAppContext, entrySet.getKey(), entrySet.getValue());
                } catch (NamingException e) {
                    throw new RuntimeException("Kunne ikke legge til connection " + e, e);
                }
            }
        }
    }

    private void addQueue(WebAppContext webAppContext) {
        if (!queues.isEmpty()) {
            for (Map.Entry<String, Queue> entrySet : queues.entrySet()) {
                try {
                    new org.eclipse.jetty.plus.jndi.Resource(webAppContext, entrySet.getKey(), entrySet.getValue());
                } catch (NamingException e) {
                    throw new RuntimeException("Kunne ikke legge til queue " + e, e);
                }
            }
        }
    }

    private Server setupJetty(final Server jetty) {
        Resource.setDefaultUseCaches(false);

        HttpConfiguration configuration = new HttpConfiguration();
        configuration.setOutputBufferSize(32768);

        ServerConnector httpConnector = new ServerConnector(jetty, new HttpConnectionFactory(configuration));
        httpConnector.setSoLingerTime(-1);
        httpConnector.setPort(port);

        Optional<ServerConnector> sslConnector = sslPort.map(new CreateSslConnector(jetty, configuration));
        List<Connector> liste = new ArrayList<>();
        liste.add(httpConnector);

        if (sslConnector.isPresent()) {
            liste.add(sslConnector.get());
        }

        Connector[] connectors = liste.toArray(new Connector[liste.size()]);
        jetty.setConnectors(connectors);
        context.setServer(jetty);
        jetty.setHandler(context);
        return jetty;
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
            if (sslPort.isPresent()) {
                LOG.info(" * Https port: " + sslPort.get());
            }
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
        if (sslPort.isPresent()) {
            urls.add(new ToUrl("https", contextPath).apply(sslPort.get()));
        }
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
