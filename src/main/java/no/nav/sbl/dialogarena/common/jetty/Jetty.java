package no.nav.sbl.dialogarena.common.jetty;

import no.nav.modig.lang.option.Optional;
import org.eclipse.jetty.jaas.JAASLoginService;
import org.eclipse.jetty.security.SecurityHandler;
import org.eclipse.jetty.server.Connector;
import org.eclipse.jetty.server.HttpConfiguration;
import org.eclipse.jetty.server.HttpConnectionFactory;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.ServerConnector;
import org.eclipse.jetty.util.resource.Resource;
import org.eclipse.jetty.webapp.FragmentConfiguration;
import org.eclipse.jetty.webapp.JettyWebXmlConfiguration;
import org.eclipse.jetty.webapp.MetaInfConfiguration;
import org.eclipse.jetty.webapp.WebAppContext;
import org.eclipse.jetty.webapp.WebInfConfiguration;
import org.eclipse.jetty.webapp.WebXmlConfiguration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.Map;

import static no.nav.modig.lang.collections.IterUtils.on;
import static no.nav.modig.lang.option.Optional.none;
import static no.nav.modig.lang.option.Optional.optional;
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

    /**
     * Builder for å konfigurere opp en Jetty-instans.
     */
    public static class JettyBuilder {
        private File war;
        private String contextPath;
        private int port = 35000;
        private Optional<Integer> sslPort = none();
        private WebAppContext context;
        private File overridewebXmlFile;
        private JAASLoginService loginService;


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
            this.sslPort = optional(sslPort);
            return this;
        }

        public final JettyBuilder overrideWebXml(File overrideWebXmlFile) {
            this.overridewebXmlFile = overrideWebXmlFile;
            return this;
        }

        public final JettyBuilder withLoginService(JAASLoginService loginService) {
            this.loginService = loginService;
            return this;
        }


        public final Jetty buildJetty() {
            try {
                if (context == null) {
                    context = new WebAppContext();
                }
                String warPath = getWarPath();
                if (isBlank(contextPath)) {
                    contextPath = getBaseName(warPath);
                }
                return new Jetty(port, sslPort, contextPath, warPath, context, overridewebXmlFile, loginService);
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
    };

    private Jetty(int port, Optional<Integer> sslPort, String contextPath, String warPath, WebAppContext context, File overrideWebXmlFile, JAASLoginService loginService) {
        this.warPath = warPath;
        this.overrideWebXmlFile = overrideWebXmlFile;

        this.port = port;
        this.sslPort = sslPort;
        this.contextPath = (contextPath.startsWith("/") ? "" : "/") + contextPath;
        this.loginService = loginService;
        this.context = setupWebapp(context);
        this.server = setupJetty(new Server());

    }

    private WebAppContext setupWebapp(final WebAppContext webAppContext) {
        if (isNotBlank(contextPath)) {
            webAppContext.setContextPath(contextPath);
        }
        if (isNotBlank(warPath)) {
            webAppContext.setWar(warPath);
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
        initParams.put("useFileMappedBuffer", "false");
        initParams.put("org.eclipse.jetty.servlet.SessionIdPathParameterName", "none"); // Forhindre url rewriting av sessionid
        webAppContext.setAttribute(WebInfConfiguration.CONTAINER_JAR_PATTERN, ".*");
        return webAppContext;
    }


    private Server setupJetty(final Server jetty) {

        Resource.setDefaultUseCaches(false);

        HttpConfiguration configuration = new HttpConfiguration();
        configuration.setOutputBufferSize(32768);

        ServerConnector httpConnector = new ServerConnector(jetty, new HttpConnectionFactory(configuration));
        httpConnector.setSoLingerTime(-1);
        httpConnector.setPort(port);


        jetty.setConnectors(on(new Connector[]{httpConnector}).append(sslPort.map(new CreateSslConnector(jetty, configuration))).collectIn(new Connector[] {}));
        context.setServer(jetty);
        jetty.setHandler(context);
        return jetty;
    }

    public Jetty start() {
        return startAnd(new Runnable() {
            @Override
            public void run() {

            }
        });
    }

    public Jetty startAnd(Runnable doWhenStarted) {
        try {
            server.start();
            LOG.info("STARTED JETTY");
            LOG.info(" * WAR: " + warPath);
            LOG.info(" * Context path: " + contextPath);
            LOG.info(" * Http port: " + port);
            for (Integer httpsPort : sslPort) {
                LOG.info(" * Https port: " + httpsPort);
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

    public final Iterable<URL> getBaseUrls() {
        return on(optional(port).map(new ToUrl("http", contextPath))).append(sslPort.map(new ToUrl("https", contextPath)));
    };

}
