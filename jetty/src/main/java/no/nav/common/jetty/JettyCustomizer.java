package no.nav.common.jetty;

import org.eclipse.jetty.server.HttpConfiguration;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.webapp.WebAppContext;

public interface JettyCustomizer {

    default void customize(HttpConfiguration httpConfiguration) {
    }
    default void customize(Server server){
    }
    default void customize(WebAppContext webAppContext){
    }

}
