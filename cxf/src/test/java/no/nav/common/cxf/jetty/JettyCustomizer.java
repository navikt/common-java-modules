package no.nav.common.cxf.jetty;

import org.eclipse.jetty.ee11.webapp.WebAppContext;
import org.eclipse.jetty.server.HttpConfiguration;
import org.eclipse.jetty.server.Server;

public interface JettyCustomizer {

    default void customize(HttpConfiguration httpConfiguration) {
    }
    default void customize(Server server){
    }
    default void customize(WebAppContext webAppContext){
    }

}
