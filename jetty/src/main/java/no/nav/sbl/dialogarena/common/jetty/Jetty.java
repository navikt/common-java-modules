package no.nav.sbl.dialogarena.common.jetty;

import org.eclipse.jetty.server.Connector;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.ServerConnector;
import org.eclipse.jetty.webapp.FragmentConfiguration;
import org.eclipse.jetty.webapp.JettyWebXmlConfiguration;
import org.eclipse.jetty.webapp.MetaInfConfiguration;
import org.eclipse.jetty.webapp.TagLibConfiguration;
import org.eclipse.jetty.webapp.WebAppContext;
import org.eclipse.jetty.webapp.WebInfConfiguration;
import org.eclipse.jetty.webapp.WebXmlConfiguration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.net.InetAddress;
import java.net.URL;
import java.util.Map;

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
        private WebAppContext context;


        public JettyBuilder war(File warPath) {
            this.war = warPath;
            return this;
        }

        public JettyBuilder at(String ctxPath) {
            this.contextPath = ctxPath;
            return this;
        }

        public JettyBuilder port(int jettyPort) {
            this.port = jettyPort;
            return this;
        }

        public Jetty buildJetty() {
            try {
                if (context == null) {
                    context = new WebAppContext();
                }
                String warPath = getWarPath();
                if (isBlank(contextPath)) {
                    contextPath = getBaseName(warPath);
                }
                return new Jetty(port, contextPath, warPath, context);
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


    public final int port;
    public final String warPath;
    public final Server server;
    public final String contextPath;
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

            MetaInfConfiguration.class.getName(),
            WebInfConfiguration.class.getName(),
            FragmentConfiguration.class.getName(),
            WebXmlConfiguration.class.getName(),
            TagLibConfiguration.class.getName(),
            JettyWebXmlConfiguration.class.getName(),

    };

    private Jetty(int port, String contextPath, String warPath, WebAppContext context) {
        this.warPath = warPath;

        this.port = port;
        this.contextPath = (contextPath.startsWith("/") ? "" : "/") + contextPath;
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

        webAppContext.setConfigurationClasses(CONFIGURATION_CLASSES);
        @SuppressWarnings("unchecked")
        Map<String, String> initParams = webAppContext.getInitParams();
        initParams.put("useFileMappedBuffer", "false");
        return webAppContext;
    }


    private Server setupJetty(final Server jetty) {
        ServerConnector connector = new ServerConnector(jetty);
        connector.setSoLingerTime(-1);
        connector.setPort(port);
        jetty.setConnectors(new Connector[]{connector});
        // Forhindre url rewriting av sessionid
        context.setInitParameter("org.eclipse.jetty.servlet.SessionIdPathParameterName", "none");
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
            LOG.info(" * Port: " + port);
            LOG.info(" * " + getBaseUrl());
            doWhenStarted.run();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        return this;
    }

    public URL getBaseUrl() {
        try {
            return new URL("http://" + InetAddress.getLocalHost().getCanonicalHostName() + ":" + port + contextPath);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }


}
