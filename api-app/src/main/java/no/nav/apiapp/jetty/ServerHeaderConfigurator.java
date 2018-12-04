package no.nav.apiapp.jetty;

import no.nav.sbl.util.EnvironmentUtils;
import org.eclipse.jetty.http.HttpGenerator;

import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;

public class ServerHeaderConfigurator implements ServletContextListener {

    @Override
    public void contextInitialized(ServletContextEvent sce) {
        HttpGenerator.setJettyVersion(EnvironmentUtils.requireApplicationName());
    }

    @Override
    public void contextDestroyed(ServletContextEvent sce) {

    }

}
